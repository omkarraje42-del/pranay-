/**
 * Loan Transactions Page Controller
 */
let currentLoanPage = 1;
let returnModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    returnModalInstance = new bootstrap.Modal(document.getElementById('returnModal'));

    loadLoans();

    const searchInput = document.getElementById('searchLoanInput');
    searchInput.addEventListener('input', UI.debounce(() => {
        currentLoanPage = 1;
        loadLoans();
    }, 350));

    document.getElementById('loanStatusFilter').addEventListener('change', () => {
        currentLoanPage = 1;
        loadLoans();
    });

    document.getElementById('resetLoanFiltersBtn').onclick = () => {
        document.getElementById('searchLoanInput').value = '';
        document.getElementById('loanStatusFilter').value = '';
        currentLoanPage = 1;
        loadLoans();
    };

    document.getElementById('returnForm').onsubmit = handleConfirmReturn;
});

async function loadLoans() {
    const tbody = document.getElementById('loansTableBody');
    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading transactions...</td></tr>';

    const search = document.getElementById('searchLoanInput').value.trim();
    const status = document.getElementById('loanStatusFilter').value;

    try {
        const resp = await API.get('/api/loans', {
            search,
            status,
            page: currentLoanPage,
            pageSize: 10
        });

        if (!resp || !resp.success || !resp.data) return;

        const { items, total, page, totalPages } = resp.data;
        document.getElementById('loanPaginationInfo').textContent = `Showing ${items.length} of ${total} transactions`;

        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center py-5">
                        <i class="bi bi-inbox fs-1 text-muted d-block mb-2"></i>
                        <h6 class="text-secondary fw-semibold">No loans found</h6>
                        <p class="text-muted small">No transactions matched your search filters</p>
                    </td>
                </tr>
            `;
            UI.renderPagination('loanPaginationContainer', 1, 1, () => {});
            return;
        }

        tbody.innerHTML = items.map(l => {
            const isLate = (l.status === 'ACTIVE' || l.status === 'OVERDUE') && l.overdueDays > 0;
            const dueDisplay = isLate ?
                `<span class="text-danger fw-bold"><i class="bi bi-clock-history me-1"></i>${UI.formatDate(l.dueDate)} (${l.overdueDays}d late)</span>` :
                UI.formatDate(l.dueDate);

            const canReturn = (l.status === 'ACTIVE' || l.status === 'OVERDUE');

            return `
                <tr>
                    <td><code>#${l.id}</code></td>
                    <td>
                        <div class="fw-bold text-dark">${l.bookTitle}</div>
                        <small class="text-muted">ISBN: ${l.isbn || 'N/A'}</small>
                    </td>
                    <td><code>${l.accessionNumber}</code></td>
                    <td>
                        <div class="fw-semibold">${l.memberName}</div>
                        <small class="text-muted">${l.membershipNumber}</small>
                    </td>
                    <td>${UI.formatDate(l.issueDate)}</td>
                    <td>${dueDisplay}</td>
                    <td>${UI.formatDate(l.returnDate)}</td>
                    <td>${UI.statusBadge(l.status)}</td>
                    <td class="text-end">
                        ${canReturn ?
                            `<button class="btn btn-sm btn-success" onclick="openReturnModal(${l.id}, '${escapeQuote(l.bookTitle)}', '${escapeQuote(l.memberName)}', '${l.accessionNumber}', '${l.dueDate}', ${l.overdueDays})" data-role="STAFF">
                                <i class="bi bi-box-arrow-in-left me-1"></i> Return
                            </button>` :
                            `<span class="badge bg-light text-muted border">Completed</span>`
                        }
                    </td>
                </tr>
            `;
        }).join('');

        UI.renderPagination('loanPaginationContainer', page, totalPages, (p) => {
            currentLoanPage = p;
            loadLoans();
        });

        Auth.applyRoleRestrictions();

    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">Error loading loans: ${e.message}</td></tr>`;
    }
}

function openReturnModal(loanId, bookTitle, memberName, accession, dueDate, overdueDays) {
    document.getElementById('returnLoanId').value = loanId;
    document.getElementById('returnBookTitle').textContent = bookTitle;
    document.getElementById('returnMemberName').textContent = 'Patron: ' + memberName;
    document.getElementById('returnAccession').textContent = accession;
    document.getElementById('returnDueDate').textContent = UI.formatDate(dueDate);
    document.getElementById('returnNotes').value = '';

    const notice = document.getElementById('overdueNotice');
    if (overdueDays > 0) {
        const estimatedFine = overdueDays * 5.0; // ₹5 per day
        notice.innerHTML = `
            <i class="bi bi-exclamation-triangle-fill text-danger me-2"></i>
            <strong>This book is ${overdueDays} day(s) overdue!</strong><br>
            An automatic fine of <strong>₹${estimatedFine.toFixed(2)}</strong> will be assessed upon return.
        `;
        notice.classList.remove('d-none');
    } else {
        notice.classList.add('d-none');
    }

    returnModalInstance.show();
}

async function handleConfirmReturn(e) {
    e.preventDefault();
    const loanId = document.getElementById('returnLoanId').value;
    const notes = document.getElementById('returnNotes').value.trim();

    const btn = document.getElementById('confirmReturnBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Processing...';

    try {
        const resp = await API.post(`/api/loans/${loanId}/return`, { notes });
        if (resp && resp.success) {
            UI.toast('Book return confirmed successfully!', 'success');
            returnModalInstance.hide();
            loadLoans();
        }
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Confirm Return';
    }
}

function escapeQuote(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}
