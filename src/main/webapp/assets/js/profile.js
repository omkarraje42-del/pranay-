// Profile & Password Management Controller
Layout.render('User Profile');

document.addEventListener('DOMContentLoaded', async () => {
    await loadUserProfile();

    // Password strength indicator
    const newPwdInput = document.getElementById('newPassword');
    newPwdInput.addEventListener('input', updatePasswordStrength);

    // Form submission
    document.getElementById('changePasswordForm').addEventListener('submit', handleChangePassword);
});

async function loadUserProfile() {
    try {
        const resp = await API.get('/api/auth/me');
        if (!resp || !resp.success) {
            window.location.href = 'login.html';
            return;
        }

        const u = resp.data;
        document.getElementById('profileFullName').textContent = u.fullName || `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.username;
        document.getElementById('profileEmail').textContent = u.email;
        document.getElementById('profileUsername').textContent = u.username;
        document.getElementById('profileFirstName').textContent = u.firstName || '—';
        document.getElementById('profileLastName').textContent = u.lastName || '—';
        
        const badge = document.getElementById('profileRoleBadge');
        badge.textContent = u.role;
        if (u.role === 'ADMIN') {
            badge.className = 'badge bg-danger-subtle text-danger px-3 py-1 fw-semibold';
        } else if (u.role === 'LIBRARIAN') {
            badge.className = 'badge bg-primary-subtle text-primary px-3 py-1 fw-semibold';
        } else {
            badge.className = 'badge bg-info-subtle text-info px-3 py-1 fw-semibold';
        }

        const avatar = document.getElementById('profileBigAvatar');
        const initial = (u.firstName ? u.firstName[0] : (u.username ? u.username[0] : 'U')).toUpperCase();
        avatar.textContent = initial;

    } catch (e) {
        UI.toast('Failed to load profile details: ' + e.message, 'error');
    }
}

function updatePasswordStrength() {
    const pwd = document.getElementById('newPassword').value;
    const bar = document.getElementById('pwdStrengthBar');
    const text = document.getElementById('pwdStrengthText');

    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    if (pwd.length === 0) {
        bar.style.width = '0%';
        bar.className = 'progress-bar';
        text.textContent = 'Password must be at least 8 characters.';
        text.className = 'text-muted';
    } else if (score <= 2) {
        bar.style.width = '30%';
        bar.className = 'progress-bar bg-danger';
        text.textContent = 'Weak password';
        text.className = 'text-danger small';
    } else if (score <= 4) {
        bar.style.width = '70%';
        bar.className = 'progress-bar bg-warning';
        text.textContent = 'Moderate password strength';
        text.className = 'text-warning small';
    } else {
        bar.style.width = '100%';
        bar.className = 'progress-bar bg-success';
        text.textContent = 'Strong password!';
        text.className = 'text-success small';
    }
}

function togglePasswordVisibility(fieldId) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    input.type = input.type === 'password' ? 'text' : 'password';
}

async function handleChangePassword(e) {
    e.preventDefault();
    const currentPassword = document.getElementById('currPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmNewPassword').value;

    if (newPassword !== confirmPassword) {
        UI.toast('New passwords do not match', 'warning');
        return;
    }

    if (newPassword.length < 8) {
        UI.toast('Password must be at least 8 characters long', 'warning');
        return;
    }

    const btn = document.getElementById('changePwdSubmitBtn');
    btn.disabled = true;

    try {
        await API.post('/api/auth/change-password', {
            currentPassword: currentPassword,
            newPassword: newPassword
        });

        UI.toast('Password changed successfully!', 'success');
        document.getElementById('changePasswordForm').reset();
        updatePasswordStrength();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}
