// User & Staff Management Controller
Layout.render('User & Staff Accounts');

let currentPage = 1;
const pageSize = 10;
let userModalInstance = null;
let resetPwdModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    // Restrict this page to ADMIN role
    const cur = Auth.getCurrentUser();
    if (!cur || cur.role !== 'ADMIN') {
        window.location.href = 'dashboard.html';
        return;
    }

    userModalInstance = new bootstrap.Modal(document.getElementById('userModal'));
    resetPwdModalInstance = new bootstrap.Modal(document.getElementById('resetPwdModal'));

    loadUsers();

    // Search debounce
    const searchInput = document.getElementById('userSearchInput');
    searchInput.addEventListener('input', UI.debounce(() => {
        currentPage = 1;
        loadUsers();
    }, 350));

    // Filters
    document.getElementById('roleFilter').addEventListener('change', () => {
        currentPage = 1;
        loadUsers();
    });
    document.getElementById('statusFilter').addEventListener('change', () => {
        currentPage = 1;
        loadUsers();
    });

    // Form handlers
    document.getElementById('userForm').addEventListener('submit', handleSaveUser);
    document.getElementById('resetPwdForm').addEventListener('submit', handleResetPassword);
});

async function loadUsers() {
    const tbody = document.getElementById('userTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading users...</td></tr>';

    const search = document.getElementById('userSearchInput').value.trim();
    const roleId = document.getElementById('roleFilter').value;
    const active = document.getElementById('statusFilter').value;

    const params = new URLSearchParams({
        page: currentPage,
        pageSize: pageSize
    });
    if (search) params.append('search', search);
    if (roleId) params.append('roleId', roleId);
    if (active) params.append('active', active);

    try {
        const resp = await API.get(`/api/users?${params.toString()}`);
        if (!resp || !resp.success) return;

        const paginated = resp.data;
        const users = paginated.items || [];

        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">No user accounts found matching criteria.</td></tr>';
            renderPagination(0, 0, 0);
            return;
        }

        tbody.innerHTML = users.map(u => {
            const roleBadgeClass = u.roleName === 'ADMIN' ? 'bg-danger-subtle text-danger' :
                                   u.roleName === 'LIBRARIAN' ? 'bg-primary-subtle text-primary' :
                                   'bg-secondary-subtle text-secondary';
            const statusBadgeClass = u.active ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-muted';
            const statusText = u.active ? 'Active' : 'Inactive';
            const createdDate = u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—';

            return `
                <tr>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <div class="user-avatar" style="width: 32px; height: 32px; font-size: 0.8rem;">
                                ${(u.firstName ? u.firstName[0] : 'U').toUpperCase()}
                            </div>
                            <div>
                                <div class="fw-semibold text-dark">${u.fullName || (u.firstName + ' ' + (u.lastName || ''))}</div>
                                <small class="text-muted">${u.email}</small>
                            </div>
                        </div>
                    </td>
                    <td><code>@${u.username}</code></td>
                    <td><span class="badge ${roleBadgeClass} fw-semibold">${u.roleName || 'USER'}</span></td>
                    <td><small class="text-muted">${u.phone || '—'}</small></td>
                    <td><span class="badge ${statusBadgeClass}">${statusText}</span></td>
                    <td><small class="text-muted">${createdDate}</small></td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-secondary" title="Edit" onclick="openEditUserModal(${u.id})">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-warning" title="Reset Password" onclick="openResetPwdModal(${u.id}, '${escapeQuote(u.username)}')">
                                <i class="bi bi-key"></i>
                            </button>
                            <button class="btn btn-outline-${u.active ? 'danger' : 'success'}" 
                                    title="${u.active ? 'Deactivate' : 'Activate'}" 
                                    onclick="toggleUserStatus(${u.id}, ${!u.active}, '${escapeQuote(u.username)}')">
                                <i class="bi bi-${u.active ? 'person-x' : 'person-check'}"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        renderPagination(paginated.currentPage, paginated.totalPages, paginated.totalItems);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">Error: ${e.message}</td></tr>`;
    }
}

function renderPagination(curr, totalPages, totalCount) {
    const container = document.getElementById('paginationContainer');
    if (totalCount === 0) {
        container.innerHTML = '<span class="text-muted small">0 accounts</span>';
        return;
    }

    let html = `<span class="text-muted small">Page ${curr} of ${totalPages} (${totalCount} total)</span>`;
    html += '<div class="btn-group btn-group-sm">';
    html += `<button class="btn btn-outline-secondary" ${curr <= 1 ? 'disabled' : ''} onclick="goToPage(${curr - 1})"><i class="bi bi-chevron-left"></i></button>`;
    
    for (let p = Math.max(1, curr - 2); p <= Math.min(totalPages, curr + 2); p++) {
        html += `<button class="btn btn-${p === curr ? 'primary' : 'outline-secondary'}" onclick="goToPage(${p})">${p}</button>`;
    }

    html += `<button class="btn btn-outline-secondary" ${curr >= totalPages ? 'disabled' : ''} onclick="goToPage(${curr + 1})"><i class="bi bi-chevron-right"></i></button>`;
    html += '</div>';

    container.innerHTML = html;
}

function goToPage(page) {
    currentPage = page;
    loadUsers();
}

function resetFilters() {
    document.getElementById('userSearchInput').value = '';
    document.getElementById('roleFilter').value = '';
    document.getElementById('statusFilter').value = '';
    currentPage = 1;
    loadUsers();
}

function openCreateUserModal() {
    document.getElementById('userModalTitle').textContent = 'Create User Account';
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('uUsername').disabled = false;
    document.getElementById('uPassword').required = true;
    document.getElementById('pwdRequiredStar').style.display = 'inline';
    document.getElementById('pwdHelp').textContent = 'Must be at least 8 characters long.';
    document.getElementById('uActive').checked = true;
}

async function openEditUserModal(id) {
    try {
        const resp = await API.get(`/api/users/${id}`);
        if (!resp || !resp.success) return;

        const u = resp.data;
        document.getElementById('userModalTitle').textContent = `Edit User: @${u.username}`;
        document.getElementById('userId').value = u.id;
        document.getElementById('uFirstName').value = u.firstName || '';
        document.getElementById('uLastName').value = u.lastName || '';
        document.getElementById('uUsername').value = u.username;
        document.getElementById('uUsername').disabled = true; // Username immutable on edit
        document.getElementById('uEmail').value = u.email;
        document.getElementById('uRole').value = u.roleId;
        document.getElementById('uPhone').value = u.phone || '';
        document.getElementById('uActive').checked = u.active;

        // Password not required on edit
        document.getElementById('uPassword').required = false;
        document.getElementById('uPassword').value = '';
        document.getElementById('pwdRequiredStar').style.display = 'none';
        document.getElementById('pwdHelp').textContent = 'Leave empty to keep existing password.';

        userModalInstance.show();
    } catch (e) {
        UI.toast(e.message, 'error');
    }
}

async function handleSaveUser(e) {
    e.preventDefault();
    const id = document.getElementById('userId').value;
    const body = {
        firstName: document.getElementById('uFirstName').value.trim(),
        lastName: document.getElementById('uLastName').value.trim(),
        username: document.getElementById('uUsername').value.trim(),
        email: document.getElementById('uEmail').value.trim(),
        roleId: parseInt(document.getElementById('uRole').value),
        phone: document.getElementById('uPhone').value.trim(),
        active: document.getElementById('uActive').checked,
        password: document.getElementById('uPassword').value
    };

    const saveBtn = document.getElementById('saveUserBtn');
    saveBtn.disabled = true;

    try {
        if (id) {
            await API.put(`/api/users/${id}`, body);
            UI.toast('User account updated successfully', 'success');
        } else {
            await API.post('/api/users', body);
            UI.toast('User created successfully', 'success');
        }
        userModalInstance.hide();
        loadUsers();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        saveBtn.disabled = false;
    }
}

function openResetPwdModal(id, username) {
    document.getElementById('resetUserId').value = id;
    document.getElementById('resetTargetUsername').textContent = username;
    document.getElementById('resetNewPassword').value = '';
    resetPwdModalInstance.show();
}

async function handleResetPassword(e) {
    e.preventDefault();
    const id = document.getElementById('resetUserId').value;
    const newPassword = document.getElementById('resetNewPassword').value;

    const btn = document.getElementById('confirmResetBtn');
    btn.disabled = true;

    try {
        await API.post(`/api/users/${id}/reset-password`, { newPassword });
        UI.toast('Password updated successfully', 'success');
        resetPwdModalInstance.hide();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

function toggleUserStatus(id, newStatus, username) {
    const actionText = newStatus ? 'activate' : 'deactivate';
    UI.confirm(
        `${newStatus ? 'Activate' : 'Deactivate'} User`,
        `Are you sure you want to ${actionText} user account "@${username}"?`,
        async () => {
            try {
                await API.patch(`/api/users/${id}/status`, { active: newStatus });
                UI.toast(`User ${actionText}d successfully`, 'success');
                loadUsers();
            } catch (err) {
                UI.toast(err.message, 'error');
            }
        }
    );
}

function escapeQuote(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}
