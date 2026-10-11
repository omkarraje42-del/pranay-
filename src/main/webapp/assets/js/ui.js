/**
 * UI Utilities, Notifications, and Shared Components
 */
const UI = {
    toast(message, type = 'success') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const icons = {
            success: 'bi-check-circle-fill text-success',
            error: 'bi-exclamation-triangle-fill text-danger',
            warning: 'bi-exclamation-circle-fill text-warning',
            info: 'bi-info-circle-fill text-primary'
        };

        const toast = document.createElement('div');
        toast.className = `toast-custom ${type}`;
        toast.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <i class="bi ${icons[type] || icons.info}"></i>
                <span>${message}</span>
            </div>
            <button type="button" class="btn-close ms-2" style="font-size: 0.7rem;"></button>
        `;

        const closeBtn = toast.querySelector('.btn-close');
        closeBtn.onclick = () => toast.remove();

        container.appendChild(toast);
        setTimeout(() => {
            toast.style.transition = 'opacity 0.4s ease';
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 400);
        }, 4000);
    },

    confirm(title, message, onConfirm) {
        let modalEl = document.getElementById('confirmModal');
        if (!modalEl) {
            modalEl = document.createElement('div');
            modalEl.id = 'confirmModal';
            modalEl.className = 'modal fade';
            modalEl.setAttribute('tabindex', '-1');
            modalEl.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="confirmModalTitle">Confirm Action</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body" id="confirmModalBody">Are you sure?</div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn btn-danger" id="confirmModalActionBtn">Confirm</button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modalEl);
        }

        document.getElementById('confirmModalTitle').textContent = title || 'Confirm Action';
        document.getElementById('confirmModalBody').textContent = message || 'Are you sure you want to proceed?';

        const modal = new bootstrap.Modal(modalEl);
        const actionBtn = document.getElementById('confirmModalActionBtn');

        const newBtn = actionBtn.cloneNode(true);
        actionBtn.parentNode.replaceChild(newBtn, actionBtn);

        newBtn.onclick = () => {
            modal.hide();
            if (typeof onConfirm === 'function') onConfirm();
        };

        modal.show();
    },

    formatCurrency(amount) {
        if (amount === null || amount === undefined) return '₹0.00';
        const num = parseFloat(amount);
        return '₹' + num.toFixed(2);
    },

    formatDate(dateStr) {
        if (!dateStr) return '—';
        try {
            const d = new Date(dateStr);
            return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
        } catch (e) {
            return dateStr;
        }
    },

    statusBadge(status) {
        if (!status) return '';
        const s = status.toUpperCase();
        let cls = 'badge-inactive';
        if (s === 'AVAILABLE' || s === 'ACTIVE' || s === 'PAID') cls = 'badge-available';
        else if (s === 'BORROWED' || s === 'READY_FOR_PICKUP') cls = 'badge-borrowed';
        else if (s === 'OVERDUE' || s === 'UNPAID' || s === 'LOST' || s === 'DAMAGED') cls = 'badge-overdue';
        else if (s === 'RETURNED' || s === 'WAIVED' || s === 'FULFILLED') cls = 'badge-returned';
        else if (s === 'RESERVED' || s === 'WAITING') cls = 'badge-reserved';

        return `<span class="badge-status ${cls}">${status}</span>`;
    },

    debounce(func, wait = 300) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), wait);
        };
    },

    renderPagination(containerId, currentPage, totalPages, onPageChange) {
        const container = document.getElementById(containerId);
        if (!container) return;

        if (totalPages <= 1) {
            container.innerHTML = '';
            return;
        }

        let html = '<ul class="pagination pagination-sm justify-content-end mb-0">';
        html += `<li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${currentPage - 1}">Previous</a>
                 </li>`;

        const startPage = Math.max(1, currentPage - 2);
        const endPage = Math.min(totalPages, currentPage + 2);

        for (let i = startPage; i <= endPage; i++) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" data-page="${i}">${i}</a>
                     </li>`;
        }

        html += `<li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${currentPage + 1}">Next</a>
                 </li>`;
        html += '</ul>';

        container.innerHTML = html;

        container.querySelectorAll('a.page-link').forEach(link => {
            link.onclick = (e) => {
                e.preventDefault();
                const p = parseInt(link.getAttribute('data-page'));
                if (p >= 1 && p <= totalPages && p !== currentPage) {
                    onPageChange(p);
                }
            };
        });
    }
};

window.UI = UI;
