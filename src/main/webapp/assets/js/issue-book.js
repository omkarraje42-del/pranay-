/**
 * Issue Book Page Controller
 */
let selectedMember = null;
let verifiedCopy = null;

document.addEventListener('DOMContentLoaded', () => {
    updateCalculatedDueDate();

    document.getElementById('loanDays').addEventListener('input', updateCalculatedDueDate);

    const memberSearch = document.getElementById('memberSearch');
    memberSearch.addEventListener('input', UI.debounce(searchMembers, 300));
    searchMembers(); // initial load of top members

    document.getElementById('memberSelect').addEventListener('change', onMemberSelected);
    document.getElementById('lookupCopyBtn').onclick = lookupCopy;
    document.getElementById('copyBarcode').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            lookupCopy();
        }
    });

    document.getElementById('issueForm').onsubmit = handleIssueBook;
});

function updateCalculatedDueDate() {
    const days = parseInt(document.getElementById('loanDays').value) || 14;
    const due = new Date();
    due.setDate(due.getDate() + days);
    document.getElementById('calculatedDueDate').value = due.toLocaleDateString('en-GB', {
        weekday: 'short', day: '2-digit', month: 'short', year: 'numeric'
    });
}

async function searchMembers() {
    const query = document.getElementById('memberSearch').value.trim();
    const select = document.getElementById('memberSelect');

    try {
        const resp = await API.get('/api/members', { search: query, pageSize: 20 });
        if (!resp || !resp.success) return;

        const members = resp.data.items || [];
        if (members.length === 0) {
            select.innerHTML = '<option value="" disabled>No matching members found</option>';
            return;
        }

        select.innerHTML = members.map(m =>
            `<option value="${m.id}" data-obj='${JSON.stringify(m).replace(/'/g, "&apos;")}'>
                ${m.fullName} (${m.membershipNumber}) - ${m.status} [${m.activeLoansCount} active loans]
            </option>`
        ).join('');

    } catch (e) {
        console.error('Error searching members:', e);
    }
}

function onMemberSelected() {
    const select = document.getElementById('memberSelect');
    const opt = select.options[select.selectedIndex];
    if (!opt || !opt.value) return;

    try {
        selectedMember = JSON.parse(opt.getAttribute('data-obj'));
    } catch (e) {
        return;
    }

    const warningBox = document.getElementById('memberWarning');
    const detailsBox = document.getElementById('patronDetails');

    let warnings = [];
    if (selectedMember.status !== 'ACTIVE') {
        warnings.push(`Patron account status is currently <strong>${selectedMember.status}</strong>.`);
    }
    if (selectedMember.activeLoansCount >= 5) {
        warnings.push(`Patron has reached the maximum loan limit of 5 active books.`);
    }
    if (parseFloat(selectedMember.unpaidFinesTotal || 0) > 50) {
        warnings.push(`Patron has unpaid fines of <strong>${UI.formatCurrency(selectedMember.unpaidFinesTotal)}</strong> blocking check-out.`);
    }

    if (warnings.length > 0) {
        warningBox.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-2"></i>' + warnings.join('<br>');
        warningBox.classList.remove('d-none');
    } else {
        warningBox.classList.add('d-none');
    }

    detailsBox.innerHTML = `
        <div class="fw-bold mb-1">${selectedMember.fullName}</div>
        <div class="text-muted mb-2">Card: <code>${selectedMember.membershipNumber}</code></div>
        <div class="d-flex justify-content-between py-1 border-bottom">
            <span>Status:</span>
            <span>${UI.statusBadge(selectedMember.status)}</span>
        </div>
        <div class="d-flex justify-content-between py-1 border-bottom">
            <span>Active Loans:</span>
            <span class="fw-bold">${selectedMember.activeLoansCount} / 5</span>
        </div>
        <div class="d-flex justify-content-between py-1 border-bottom">
            <span>Unpaid Fines:</span>
            <span class="fw-bold ${selectedMember.unpaidFinesTotal > 0 ? 'text-danger' : 'text-success'}">${UI.formatCurrency(selectedMember.unpaidFinesTotal)}</span>
        </div>
        <div class="d-flex justify-content-between py-1">
            <span>Expiry Date:</span>
            <span>${UI.formatDate(selectedMember.membershipExpiryDate)}</span>
        </div>
    `;
}

async function lookupCopy() {
    const barcode = document.getElementById('copyBarcode').value.trim();
    if (!barcode) {
        UI.toast('Please enter an Accession Barcode', 'warning');
        return;
    }

    const detailsBox = document.getElementById('copyDetailsBox');

    try {
        // Query loans or books with search barcode
        const resp = await API.get('/api/books', { search: barcode });
        if (!resp || !resp.success || !resp.data.items || resp.data.items.length === 0) {
            UI.toast('Barcode not found in catalog', 'error');
            detailsBox.classList.add('d-none');
            verifiedCopy = null;
            return;
        }

        const book = resp.data.items[0];
        // Fetch copy list
        const copiesResp = await API.get(`/api/books/${book.id}/copies`);
        const copy = (copiesResp.data || []).find(c => c.accessionNumber.toLowerCase() === barcode.toLowerCase());

        if (!copy) {
            UI.toast(`Copy ${barcode} not found for book '${book.title}'`, 'error');
            verifiedCopy = null;
            return;
        }

        verifiedCopy = copy;
        verifiedCopy.bookTitle = book.title;

        detailsBox.classList.remove('d-none');
        document.getElementById('copyBookTitle').textContent = book.title;
        document.getElementById('copyBarcodeText').textContent = `Accession: ${copy.accessionNumber} | Condition: ${copy.condition}`;
        document.getElementById('copyStatusBadge').innerHTML = UI.statusBadge(copy.status);

        if (copy.status !== 'AVAILABLE' && copy.status !== 'RESERVED') {
            UI.toast(`Warning: Copy is currently marked as ${copy.status}`, 'warning');
        }

    } catch (e) {
        UI.toast('Lookup error: ' + e.message, 'error');
    }
}

async function handleIssueBook(e) {
    e.preventDefault();
    if (!selectedMember) {
        UI.toast('Please select a member first', 'warning');
        return;
    }

    const barcode = document.getElementById('copyBarcode').value.trim();
    if (!barcode) {
        UI.toast('Please scan or enter a physical copy barcode', 'warning');
        return;
    }

    const loanDays = parseInt(document.getElementById('loanDays').value) || 14;
    const notes = document.getElementById('issueNotes').value.trim();

    const submitBtn = document.getElementById('submitIssueBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Processing Issue...';

    try {
        const resp = await API.post('/api/loans/issue', {
            memberId: selectedMember.id,
            accessionNumber: barcode,
            loanDays: loanDays,
            notes: notes
        });

        if (resp && resp.success) {
            UI.toast(`Book '${resp.data.bookTitle}' successfully issued to ${resp.data.memberName}!`, 'success');
            // Reset form
            document.getElementById('issueForm').reset();
            document.getElementById('copyDetailsBox').classList.add('d-none');
            document.getElementById('memberWarning').classList.add('d-none');
            document.getElementById('patronDetails').innerHTML = '<div class="text-muted text-center py-4">No member selected yet</div>';
            selectedMember = null;
            verifiedCopy = null;
            updateCalculatedDueDate();
        }
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Issue Book';
    }
}
