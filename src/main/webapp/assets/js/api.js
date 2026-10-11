/**
 * Centralized REST API Client
 */
const API = {
    baseUrl: '',

    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(options.headers || {})
        };

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Handle HTTP 401 Unauthorized
            if (response.status === 401) {
                if (!window.location.pathname.endsWith('login.html')) {
                    sessionStorage.setItem('redirectAfterLogin', window.location.pathname + window.location.search);
                    window.location.href = 'login.html';
                }
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.message || 'Authentication required');
            }

            // Handle HTTP 403 Forbidden
            if (response.status === 403) {
                const errData = await response.json().catch(() => ({}));
                const msg = errData.message || 'Access denied: insufficient permissions';
                if (window.UI) window.UI.toast(msg, 'error');
                throw new Error(msg);
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                const errorMsg = data?.message || (data?.errors && data.errors.join(', ')) || `HTTP ${response.status}: Request failed`;
                throw new Error(errorMsg);
            }

            return data;

        } catch (error) {
            console.error(`API Error [${options.method || 'GET'} ${endpoint}]:`, error);
            throw error;
        }
    },

    get(endpoint, params = {}) {
        const queryString = new URLSearchParams(
            Object.entries(params).filter(([_, v]) => v !== null && v !== undefined && v !== '')
        ).toString();
        const url = queryString ? `${endpoint}?${queryString}` : endpoint;
        return this.request(url, { method: 'GET' });
    },

    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    put(endpoint, body) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    patch(endpoint, body) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: JSON.stringify(body)
        });
    },

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
};

window.API = API;
