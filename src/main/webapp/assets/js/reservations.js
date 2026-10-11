/**
 * Reservations Queue Page Controller
 */
let currentResPage = 1;
let resModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    resModalInstance = new bootstrap.Modal(document.getElementById('reservationModal'));

    loadReservations();
    loadBooksForRes();
    loadMembersForRes();

    document.getElementById('resStatusFilter').addEventListener('change', () => {
        currentResPage = 1;
        loadReservations();
    });

    document.getElementById('reservationForm').onsubmit = handlePlaceReservation;
});

async function loadBooksForRes() {
    try {
        const resp = await API.get('/api/books', { pageSize: 100 });
        if (resp && resp.success) {
            const select = document.getElementById('resBookSelect');
            select.innerHTML = '<option value="">Select Book Title</option>' +
                resp.data.items.map(b => `<option value="${b.id}">${b.title} (${b.availableCopies} available / ${b.totalCopies} total)</option>`).join('');
        }
    } catch (e) {}
}

async function loadMembersForRes() {
    try {
        const resp = await API.get('/api/members', { pageSize: 100, status: 'ACTIVE' });
        if (resp && resp.success) {
            const select = document.getElementById('resMemberSelect');
            select.innerHTML = '<option value="">Select Member</option>' +
                resp.data.items.map(m => `<option value="${m.id}">${m.fullName} (${m.membershipNumber})</option>`).join('');
        }
    } catch (e) {}
}

async function loadReservations() {
    const tbody = document.getElementById('reservationsTableBody');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading queue...</td></tr>';

    const status = document.getElementById('resStatusFilter').value;

    try {
        const resp = await API.get('/api/reservations', {
            status,
            page: currentResPage,
            pageSize: 10
        });

        if (!resp || !resp.success || !resp.data) return;

        const { items, total, page, totalPages } = resp.data;
        document.getElementById('resPaginationInfo').textContent = `Showing ${items.length} of ${total} reservations`;

        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5">
                        <i class="bi bi-clock-history fs-1 text-muted d-block mb-2"></i>
                        <h6 class="text-secondary fw-semibold">No reservations in queue</h6>
                    </td>
                </tr>
            `;
            UI.renderPagination('resPaginationContainer', 1, 1, () => {});
            return;
        }

        tbody.innerHTML = items.map(r => `
            <tr>
                <td><code>#${r.id}</code></td>
                <td>
                    <div class="fw-bold text-dark">${r.bookTitle}</div>
                    <small class="text-muted">ISBN: ${r.isbn || '—'}</small>
                </td>
                <td>
                    <div class="fw-semibold">${r.memberName}</div>
                    <small class="text-muted">${r.membershipNumber}</small>
                </td>
                <td>${UI.formatDate(r.reservationDate)}</td>
                <td class="text-center"><span class="badge bg-light text-secondary border">Priority ${r.priority}</span></td>
                <td>${UI.statusBadge(r.status)}</td>
                <td>${r.pickupDeadline ? `<span class="text-danger fw-semibold">${UI.formatDate(r.pickupDeadline)}</span>` : '—'}</td>
                <td class="text-end">
                    ${r.status === 'WAITING' || r.status === 'READY_FOR_PICKUP' ?
                        `<button class="btn btn-sm btn-outline-danger" onclick="cancelReservation(${r.id}, '${escapeQuote(r.bookTitle)}')">
                            <i class="bi bi-x-circle me-1"></i> Cancel
                        </button>` :
                        '<span class="badge bg-light text-muted border">Closed</span>'
                    }
                </td>
            </tr>
        `).join('');

        UI.renderPagination('resPaginationContainer', page, totalPages, (p) => {
            currentResPage = p;
            loadReservations();
        });

    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Error loading queue: ${e.message}</td></tr>`;
    }
}

async function handlePlaceReservation(e) {
    e.preventDefault();
    const bookId = document.getElementById('resBookSelect').value;
    const memberId = document.getElementById('resMemberSelect').value;
    const notes = document.getElementById('resNotes').value.trim();

    if (!bookId || !memberId) {
        UI.toast('Please select both book and member', 'warning');
        return;
    }

    const btn = document.getElementById('submitResBtn');
    btn.disabled = true;

    try {
        const resp = await API.post('/api/reservations', {
            bookId: parseInt(bookId),
            memberId: parseInt(memberId),
            notes: notes || null
        });

        if (resp && resp.success) {
            UI.toast('Reservation placed successfully in queue!', 'success');
            resModalInstance.hide();
            document.getElementById('reservationForm').reset();
            loadReservations();
        }
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

function cancelReservation(id, bookTitle) {
    UI.confirm(
        'Cancel Hold Reservation',
        `Are you sure you want to cancel the hold reservation for "${bookTitle}"?`,
        async () => {
            try {
                await API.patch(`/api/reservations/${id}/cancel`, {});
                UI.toast('Reservation cancelled successfully', 'success');
                loadReservations();
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
