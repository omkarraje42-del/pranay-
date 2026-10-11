/**
 * Authentication and Role-based UI State Manager
 */
const Auth = {
    currentUser: null,

    async init() {
        const isLoginPage = window.location.pathname.endsWith('login.html');

        try {
            const resp = await API.get('/api/auth/me');
            if (resp && resp.success && resp.data) {
                this.currentUser = resp.data;
                sessionStorage.setItem('currentUser', JSON.stringify(this.currentUser));
                this.applyRoleRestrictions();
                this.updateUserUI();

                if (isLoginPage) {
                    window.location.href = 'dashboard.html';
                }
            } else if (!isLoginPage) {
                window.location.href = 'login.html';
            }
        } catch (e) {
            if (!isLoginPage) {
                window.location.href = 'login.html';
            }
        }
    },

    getCurrentUser() {
        if (!this.currentUser) {
            const stored = sessionStorage.getItem('currentUser');
            if (stored) {
                try { this.currentUser = JSON.parse(stored); } catch (e) {}
            }
        }
        return this.currentUser;
    },

    applyRoleRestrictions() {
        const user = this.getCurrentUser();
        if (!user) return;

        const role = (user.role || 'MEMBER').toUpperCase();

        document.querySelectorAll('[data-role]').forEach(el => {
            const required = el.getAttribute('data-role').toUpperCase();
            if (required === 'ADMIN' && role !== 'ADMIN') {
                el.style.display = 'none';
            } else if (required === 'STAFF' && role !== 'ADMIN' && role !== 'LIBRARIAN') {
                el.style.display = 'none';
            }
        });
    },

    updateUserUI() {
        const user = this.getCurrentUser();
        if (!user) return;

        const nameEl = document.getElementById('navUserName');
        if (nameEl) nameEl.textContent = user.fullName || user.username;

        const roleEl = document.getElementById('navUserRole');
        if (roleEl) roleEl.textContent = user.role;

        // Hook logout button
        const logoutBtn = document.getElementById('btnLogout');
        if (logoutBtn) {
            logoutBtn.onclick = (e) => {
                e.preventDefault();
                this.logout();
            };
        }
    },

    async logout() {
        try {
            await API.post('/api/auth/logout', {});
        } catch (e) {
            console.error('Logout error', e);
        } finally {
            sessionStorage.removeItem('currentUser');
            window.location.href = 'login.html';
        }
    }
};

window.Auth = Auth;

// Auto-initialize authentication on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    Auth.init();
});
