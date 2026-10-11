/**
 * Fines & Financials Page Controller
 */
let currentFinePage = 1;
let payModalInstance = null;
let waiveModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    payModalInstance = new bootstrap.Modal(document.getElementById('payModal'));
    waiveModalInstance = new bootstrap.Modal(document.getElementById('waiveModal'));

    loadFines();

    document.getElementById('fineStatusFilter').addEventListener('change', () => {
        currentFinePage = 1;
        loadFines();
    });

    document.getElementById('payForm').onsubmit = handleConfirmPay;
    document.getElementById('waiveForm').onsubmit = handleConfirmWaive;
});

async function loadFines() {
    const tbody = document.getElementById('finesTableBody');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading fines...</td></tr>';

    const status = document.getElementById('fineStatusFilter').value;

    try {
        const resp = await API.get('/api/fines', {
            status,
            page: currentFinePage,
            pageSize: 10
        });

        if (!resp || !resp.success || !resp.data) return;

        const { items, total, page, totalPages } = resp.data;
        document.getElementById('finePaginationInfo').textContent = `Showing ${items.length} of ${total} records`;

        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5">
                        <i class="bi bi-wallet2 fs-1 text-muted d-block mb-2"></i>
                        <h6 class="text-secondary fw-semibold">No fine records found</h6>
                    </td>
                </tr>
            `;
            UI.renderPagination('finePaginationContainer', 1, 1, () => {});
            return;
        }

        tbody.innerHTML = items.map(f => {
            const isUnpaid = f.status === 'UNPAID';
            return `
                <tr>
                    <td><code>#${f.id}</code></td>
                    <td>
                        <div class="fw-bold text-dark">${f.memberName}</div>
                        <small class="text-muted">${f.membershipNumber}</small>
                    </td>
                    <td>
                        <div>${f.bookTitle || 'Loan #' + f.loanId}</div>
                        <small class="text-muted">Copy: ${f.accessionNumber || '—'}</small>
                    </td>
                    <td><small class="text-muted">${f.reason}</small></td>
                    <td class="text-center fw-bold text-danger fs-6">${UI.formatCurrency(f.amount)}</td>
                    <td><small>${UI.formatDate(f.createdAt)}</small></td>
                    <td>${UI.statusBadge(f.status)}</td>
                    <td class="text-end">
                        ${isUnpaid ? `
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-success" onclick="openPayModal(${f.id}, '${escapeQuote(f.memberName)}', '${f.amount}')" data-role="STAFF">
                                    <i class="bi bi-cash me-1"></i> Pay
                                </button>
                                <button class="btn btn-outline-warning text-dark" onclick="openWaiveModal(${f.id}, '${escapeQuote(f.memberName)}', '${f.amount}')" data-role="STAFF">
                                    <i class="bi bi-shield-x me-1"></i> Waive
                                </button>
                            </div>
                        ` : `
                            <small class="text-muted">${f.status === 'PAID' ? 'Rcpt: ' + (f.receiptNumber || 'N/A') : 'Waived: ' + (f.waiverReason || 'Authorized')}</small>
                        `}
                    </td>
                </tr>
            `;
        }).join('');

        UI.renderPagination('finePaginationContainer', page, totalPages, (p) => {
            currentFinePage = p;
            loadFines();
        });

        Auth.applyRoleRestrictions();

    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Error loading fines: ${e.message}</td></tr>`;
    }
}

function openPayModal(fineId, memberName, amount) {
    document.getElementById('payFineId').value = fineId;
    document.getElementById('payMemberName').textContent = memberName;
    document.getElementById('payAmount').textContent = UI.formatCurrency(amount);
    document.getElementById('payReceipt').value = 'RCPT-' + Date.now().toString().slice(-6);
    payModalInstance.show();
}

async function handleConfirmPay(e) {
    e.preventDefault();
    const fineId = document.getElementById('payFineId').value;
    const paymentMethod = document.getElementById('payMethod').value;
    const receiptNumber = document.getElementById('payReceipt').value.trim();

    const btn = document.getElementById('confirmPayBtn');
    btn.disabled = true;

    try {
        await API.post(`/api/fines/${fineId}/pay`, { paymentMethod, receiptNumber });
        UI.toast('Fine payment recorded successfully', 'success');
        payModalInstance.hide();
        loadFines();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

function openWaiveModal(fineId, memberName, amount) {
    document.getElementById('waiveFineId').value = fineId;
    document.getElementById('waiveMemberName').textContent = memberName;
    document.getElementById('waiveAmount').textContent = UI.formatCurrency(amount);
    document.getElementById('waiveReason').value = '';
    waiveModalInstance.show();
}

async function handleConfirmWaive(e) {
    e.preventDefault();
    const fineId = document.getElementById('waiveFineId').value;
    const reason = document.getElementById('waiveReason').value.trim();

    if (!reason) {
        UI.toast('Please provide a reason for waiving this fine', 'warning');
        return;
    }

    const btn = document.getElementById('confirmWaiveBtn');
    btn.disabled = true;

    try {
        await API.post(`/api/fines/${fineId}/waive`, { reason });
        UI.toast('Fine waived successfully and logged', 'success');
        waiveModalInstance.hide();
        loadFines();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

function escapeQuote(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}
