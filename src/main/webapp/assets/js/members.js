/**
 * Members Management Page Controller
 */
let currentMemberPage = 1;
let memberModalInstance = null;
let patronLoansModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    memberModalInstance = new bootstrap.Modal(document.getElementById('memberModal'));
    patronLoansModalInstance = new bootstrap.Modal(document.getElementById('patronLoansModal'));

    loadMembers();

    const searchInput = document.getElementById('searchMemberInput');
    searchInput.addEventListener('input', UI.debounce(() => {
        currentMemberPage = 1;
        loadMembers();
    }, 350));

    document.getElementById('statusFilter').addEventListener('change', () => {
        currentMemberPage = 1;
        loadMembers();
    });

    document.getElementById('resetMemberFiltersBtn').onclick = () => {
        document.getElementById('searchMemberInput').value = '';
        document.getElementById('statusFilter').value = '';
        currentMemberPage = 1;
        loadMembers();
    };

    document.getElementById('memberForm').onsubmit = handleSaveMember;
});

async function loadMembers() {
    const tbody = document.getElementById('membersTableBody');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading members...</td></tr>';

    const search = document.getElementById('searchMemberInput').value.trim();
    const status = document.getElementById('statusFilter').value;

    try {
        const resp = await API.get('/api/members', {
            search,
            status,
            page: currentMemberPage,
            pageSize: 10
        });

        if (!resp || !resp.success || !resp.data) return;

        const { items, total, page, totalPages } = resp.data;
        document.getElementById('memberPaginationInfo').textContent = `Showing ${items.length} of ${total} members`;

        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5">
                        <i class="bi bi-people fs-1 text-muted d-block mb-2"></i>
                        <h6 class="text-secondary fw-semibold">No members found</h6>
                        <p class="text-muted small">Try modifying search or filter criteria</p>
                    </td>
                </tr>
            `;
            UI.renderPagination('memberPaginationContainer', 1, 1, () => {});
            return;
        }

        tbody.innerHTML = items.map(m => {
            const hasFines = m.unpaidFinesTotal && parseFloat(m.unpaidFinesTotal) > 0;
            const fineBadge = hasFines ?
                `<span class="badge bg-danger-subtle text-danger fw-bold">${UI.formatCurrency(m.unpaidFinesTotal)}</span>` :
                `<span class="text-muted small">₹0.00</span>`;

            return `
                <tr>
                    <td><code>${m.membershipNumber}</code></td>
                    <td>
                        <div class="fw-bold text-dark">${m.fullName}</div>
                        <small class="text-muted">${m.address || 'No address'}</small>
                    </td>
                    <td>
                        <div><i class="bi bi-envelope text-muted me-1"></i>${m.email}</div>
                        <small class="text-muted"><i class="bi bi-telephone text-muted me-1"></i>${m.phone || '—'}</small>
                    </td>
                    <td>
                        <div class="small">Start: ${UI.formatDate(m.membershipStartDate)}</div>
                        <small class="text-muted">Exp: ${UI.formatDate(m.membershipExpiryDate)}</small>
                    </td>
                    <td class="text-center">
                        <span class="badge ${m.activeLoansCount > 0 ? 'bg-primary-subtle text-primary' : 'bg-light text-secondary'}">
                            ${m.activeLoansCount} active
                        </span>
                    </td>
                    <td class="text-center">${fineBadge}</td>
                    <td>${UI.statusBadge(m.status)}</td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-info" title="View History & Loans" onclick="openPatronLoansModal(${m.id}, '${escapeQuote(m.fullName)}')">
                                <i class="bi bi-clock-history"></i>
                            </button>
                            <button class="btn btn-outline-secondary" title="Edit Member" onclick="openEditMemberModal(${m.id})" data-role="STAFF">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-warning dropdown-toggle" data-bs-toggle="dropdown" title="Change Status" data-role="STAFF">
                                <i class="bi bi-gear"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end shadow-sm small">
                                <li><a class="dropdown-item ${m.status === 'ACTIVE' ? 'active' : ''}" href="#" onclick="changeMemberStatus(${m.id}, 'ACTIVE')">Set Active</a></li>
                                <li><a class="dropdown-item ${m.status === 'SUSPENDED' ? 'active' : ''}" href="#" onclick="changeMemberStatus(${m.id}, 'SUSPENDED')">Set Suspended</a></li>
                                <li><a class="dropdown-item ${m.status === 'INACTIVE' ? 'active' : ''}" href="#" onclick="changeMemberStatus(${m.id}, 'INACTIVE')">Set Inactive</a></li>
                            </ul>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        UI.renderPagination('memberPaginationContainer', page, totalPages, (p) => {
            currentMemberPage = p;
            loadMembers();
        });

        Auth.applyRoleRestrictions();

    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Error loading members: ${e.message}</td></tr>`;
    }
}

function openRegisterMemberModal() {
    document.getElementById('memberModalTitle').textContent = 'Register New Member';
    document.getElementById('memberForm').reset();
    document.getElementById('memberId').value = '';

    // Default expiry 1 year from today
    const now = new Date();
    now.setFullYear(now.getFullYear() + 1);
    document.getElementById('memberExpiryDate').value = now.toISOString().split('T')[0];
}

async function openEditMemberModal(memberId) {
    try {
        const resp = await API.get(`/api/members/${memberId}`);
        if (!resp || !resp.success) return;
        const m = resp.data;

        document.getElementById('memberModalTitle').textContent = 'Edit Member: ' + m.fullName;
        document.getElementById('memberId').value = m.id;
        document.getElementById('memberFirstName').value = m.firstName || '';
        document.getElementById('memberLastName').value = m.lastName || '';
        document.getElementById('memberEmail').value = m.email || '';
        document.getElementById('memberPhone').value = m.phone || '';
        document.getElementById('memberCardNumber').value = m.membershipNumber || '';
        document.getElementById('memberExpiryDate').value = m.membershipExpiryDate || '';
        document.getElementById('memberAddress').value = m.address || '';

        memberModalInstance.show();
    } catch (e) {
        UI.toast('Failed to load member: ' + e.message, 'error');
    }
}

async function handleSaveMember(e) {
    e.preventDefault();
    const id = document.getElementById('memberId').value;

    const payload = {
        firstName: document.getElementById('memberFirstName').value.trim(),
        lastName: document.getElementById('memberLastName').value.trim(),
        email: document.getElementById('memberEmail').value.trim(),
        phone: document.getElementById('memberPhone').value.trim() || null,
        membershipNumber: document.getElementById('memberCardNumber').value.trim() || null,
        membershipExpiryDate: document.getElementById('memberExpiryDate').value,
        address: document.getElementById('memberAddress').value.trim() || null
    };

    const saveBtn = document.getElementById('saveMemberBtn');
    saveBtn.disabled = true;

    try {
        if (id) {
            await API.put(`/api/members/${id}`, payload);
            UI.toast('Member updated successfully', 'success');
        } else {
            await API.post('/api/members', payload);
            UI.toast('Member registered successfully', 'success');
        }
        memberModalInstance.hide();
        loadMembers();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        saveBtn.disabled = false;
    }
}

async function changeMemberStatus(memberId, status) {
    try {
        await API.patch(`/api/members/${memberId}/status`, { status });
        UI.toast(`Member status updated to ${status}`, 'success');
        loadMembers();
    } catch (err) {
        UI.toast(err.message, 'error');
    }
}

async function openPatronLoansModal(memberId, memberName) {
    document.getElementById('patronLoansModalTitle').textContent = `Loan History: ${memberName}`;
    const tbody = document.getElementById('patronLoansTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">Loading history...</td></tr>';
    patronLoansModalInstance.show();

    try {
        const resp = await API.get(`/api/members/${memberId}/loans`);
        if (!resp || !resp.success) return;

        const loans = resp.data.items || [];
        if (loans.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">No borrowing transactions recorded.</td></tr>';
            return;
        }

        tbody.innerHTML = loans.map(l => `
            <tr>
                <td class="fw-semibold">${l.bookTitle}</td>
                <td><code>${l.accessionNumber}</code></td>
                <td>${UI.formatDate(l.issueDate)}</td>
                <td>${UI.formatDate(l.dueDate)}</td>
                <td>${UI.formatDate(l.returnDate)}</td>
                <td>${UI.statusBadge(l.status)}</td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-3">Error: ${err.message}</td></tr>`;
    }
}

function escapeQuote(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}
