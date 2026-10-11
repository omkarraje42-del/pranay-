/**
 * Books Catalog Page Controller
 */
let currentPage = 1;
let currentBookForCopies = null;
let bookModalInstance = null;
let copiesModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    bookModalInstance = new bootstrap.Modal(document.getElementById('bookModal'));
    copiesModalInstance = new bootstrap.Modal(document.getElementById('copiesModal'));

    loadCategories();
    loadAuthors();
    loadPublishers();
    loadBooks();

    // Setup filter listeners
    const searchInput = document.getElementById('searchInput');
    searchInput.addEventListener('input', UI.debounce(() => {
        currentPage = 1;
        loadBooks();
    }, 350));

    document.getElementById('categoryFilter').addEventListener('change', () => { currentPage = 1; loadBooks(); });
    document.getElementById('availabilityFilter').addEventListener('change', () => { currentPage = 1; loadBooks(); });
    document.getElementById('sortBy').addEventListener('change', () => { currentPage = 1; loadBooks(); });
    document.getElementById('resetFiltersBtn').onclick = resetFilters;

    document.getElementById('bookForm').onsubmit = handleSaveBook;
    document.getElementById('addCopyForm').onsubmit = handleAddCopy;
});

function resetFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('categoryFilter').value = '';
    document.getElementById('availabilityFilter').value = '';
    document.getElementById('sortBy').value = 'title';
    currentPage = 1;
    loadBooks();
}

async function loadCategories() {
    try {
        const resp = await API.get('/api/categories');
        if (resp && resp.success) {
            const selectFilter = document.getElementById('categoryFilter');
            const selectModal = document.getElementById('bookCategory');
            const opts = resp.data.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
            selectFilter.innerHTML = '<option value="">All Categories</option>' + opts;
            selectModal.innerHTML = '<option value="">Select Category</option>' + opts;
        }
    } catch (e) {
        console.error('Error loading categories:', e);
    }
}

async function loadAuthors() {
    try {
        const resp = await API.get('/api/authors', { all: true });
        if (resp && resp.success) {
            const select = document.getElementById('bookAuthors');
            select.innerHTML = resp.data.map(a => `<option value="${a.id}">${a.fullName} (${a.nationality || 'Intl'})</option>`).join('');
        }
    } catch (e) {
        console.error('Error loading authors:', e);
    }
}

async function loadPublishers() {
    try {
        const resp = await API.get('/api/publishers');
        if (resp && resp.success) {
            const select = document.getElementById('bookPublisher');
            select.innerHTML = '<option value="">Select Publisher</option>' +
                resp.data.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
        }
    } catch (e) {
        console.error('Error loading publishers:', e);
    }
}

async function loadBooks() {
    const tbody = document.getElementById('booksTableBody');
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading catalog...</td></tr>';

    const search = document.getElementById('searchInput').value.trim();
    const categoryId = document.getElementById('categoryFilter').value;
    const availability = document.getElementById('availabilityFilter').value;
    const sortBy = document.getElementById('sortBy').value;

    try {
        const resp = await API.get('/api/books', {
            search,
            categoryId,
            availability,
            sortBy,
            page: currentPage,
            pageSize: 10
        });

        if (!resp || !resp.success || !resp.data) return;

        const { items, total, page, totalPages } = resp.data;
        document.getElementById('paginationInfo').textContent = `Showing ${items.length} of ${total} books`;

        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5">
                        <i class="bi bi-journal-x fs-1 text-muted d-block mb-2"></i>
                        <h6 class="text-secondary fw-semibold">No books match your criteria</h6>
                        <p class="text-muted small">Try modifying your search or filter terms</p>
                    </td>
                </tr>
            `;
            UI.renderPagination('paginationContainer', 1, 1, () => {});
            return;
        }

        tbody.innerHTML = items.map(b => {
            const cover = b.coverImageUrl ?
                `<img src="${b.coverImageUrl}" alt="cover" class="rounded" style="width: 38px; height: 50px; object-fit: cover;">` :
                `<div class="rounded bg-light d-flex align-items-center justify-content-center text-secondary border" style="width: 38px; height: 50px;"><i class="bi bi-book"></i></div>`;

            const availBadge = b.availableCopies > 0 ?
                `<span class="badge bg-success-subtle text-success">${b.availableCopies} avail</span>` :
                `<span class="badge bg-danger-subtle text-danger">0 avail</span>`;

            return `
                <tr>
                    <td>${cover}</td>
                    <td>
                        <div class="fw-bold text-dark">${b.title}</div>
                        ${b.subtitle ? `<div class="small text-muted text-truncate" style="max-width: 280px;">${b.subtitle}</div>` : ''}
                        <div class="small text-secondary">Pub: ${b.publicationYear || 'N/A'} | ${b.edition || '1st Ed.'}</div>
                    </td>
                    <td><small class="text-dark">${b.authorsString || 'Unassigned'}</small></td>
                    <td><span class="badge bg-light text-secondary border">${b.categoryName || 'General'}</span></td>
                    <td><code>${b.isbn || '—'}</code></td>
                    <td><small class="text-muted"><i class="bi bi-geo-alt me-1"></i>${b.shelfLocation || 'Unassigned'}</small></td>
                    <td class="text-center">
                        <div>${availBadge}</div>
                        <small class="text-muted">Total: ${b.totalCopies}</small>
                    </td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-primary" title="Manage Physical Copies" onclick="openCopiesModal(${b.id}, '${escapeQuote(b.title)}')">
                                <i class="bi bi-copy"></i> Copies
                            </button>
                            <button class="btn btn-outline-secondary" title="Edit Book" onclick="openEditBookModal(${b.id})" data-role="STAFF">
                                <i class="bi bi-pencil"></i>
                            </button>
                            ${b.active ?
                                `<button class="btn btn-outline-danger" title="Deactivate" onclick="toggleBookActive(${b.id}, false, '${escapeQuote(b.title)}')" data-role="STAFF">
                                    <i class="bi bi-slash-circle"></i>
                                </button>` :
                                `<button class="btn btn-outline-success" title="Activate" onclick="toggleBookActive(${b.id}, true, '${escapeQuote(b.title)}')" data-role="STAFF">
                                    <i class="bi bi-check-circle"></i>
                                </button>`
                            }
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        UI.renderPagination('paginationContainer', page, totalPages, (newPage) => {
            currentPage = newPage;
            loadBooks();
        });

        Auth.applyRoleRestrictions();

    } catch (e) {
        console.error('Error loading books:', e);
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">Error loading books: ${e.message}</td></tr>`;
    }
}

function openCreateBookModal() {
    document.getElementById('bookModalTitle').textContent = 'Add New Book';
    document.getElementById('bookForm').reset();
    document.getElementById('bookId').value = '';
    document.getElementById('initialCopiesGroup').style.display = 'block';
}

async function openEditBookModal(bookId) {
    try {
        const resp = await API.get(`/api/books/${bookId}`);
        if (!resp || !resp.success) return;
        const b = resp.data;

        document.getElementById('bookModalTitle').textContent = 'Edit Book: ' + b.title;
        document.getElementById('bookId').value = b.id;
        document.getElementById('bookTitle').value = b.title || '';
        document.getElementById('bookIsbn').value = b.isbn || '';
        document.getElementById('bookSubtitle').value = b.subtitle || '';
        document.getElementById('bookCategory').value = b.categoryId || '';
        document.getElementById('bookPublisher').value = b.publisherId || '';
        document.getElementById('bookYear').value = b.publicationYear || '';
        document.getElementById('bookEdition').value = b.edition || '';
        document.getElementById('bookShelf').value = b.shelfLocation || '';
        document.getElementById('bookLanguage').value = b.language || 'English';
        document.getElementById('bookCoverUrl').value = b.coverImageUrl || '';
        document.getElementById('bookDescription').value = b.description || '';

        // Select authors
        const authorSelect = document.getElementById('bookAuthors');
        const selectedAuthorIds = (b.authors || []).map(a => a.id.toString());
        Array.from(authorSelect.options).forEach(opt => {
            opt.selected = selectedAuthorIds.includes(opt.value);
        });

        document.getElementById('initialCopiesGroup').style.display = 'none';
        bookModalInstance.show();
    } catch (e) {
        UI.toast('Failed to load book details: ' + e.message, 'error');
    }
}

async function handleSaveBook(e) {
    e.preventDefault();
    const id = document.getElementById('bookId').value;
    const authorSelect = document.getElementById('bookAuthors');
    const authorIds = Array.from(authorSelect.selectedOptions).map(o => parseInt(o.value));

    const payload = {
        title: document.getElementById('bookTitle').value.trim(),
        isbn: document.getElementById('bookIsbn').value.trim() || null,
        subtitle: document.getElementById('bookSubtitle').value.trim() || null,
        categoryId: parseInt(document.getElementById('bookCategory').value),
        publisherId: document.getElementById('bookPublisher').value ? parseInt(document.getElementById('bookPublisher').value) : null,
        publicationYear: document.getElementById('bookYear').value ? parseInt(document.getElementById('bookYear').value) : null,
        edition: document.getElementById('bookEdition').value.trim() || null,
        shelfLocation: document.getElementById('bookShelf').value.trim() || null,
        language: document.getElementById('bookLanguage').value.trim() || 'English',
        coverImageUrl: document.getElementById('bookCoverUrl').value.trim() || null,
        description: document.getElementById('bookDescription').value.trim() || null,
        authorIds: authorIds,
        initialCopies: parseInt(document.getElementById('bookInitialCopies').value) || 1,
        active: true
    };

    const saveBtn = document.getElementById('saveBookBtn');
    saveBtn.disabled = true;

    try {
        if (id) {
            await API.put(`/api/books/${id}`, payload);
            UI.toast('Book updated successfully', 'success');
        } else {
            await API.post('/api/books', payload);
            UI.toast('Book created successfully with physical copies', 'success');
        }
        bookModalInstance.hide();
        loadBooks();
    } catch (err) {
        UI.toast(err.message, 'error');
    } finally {
        saveBtn.disabled = false;
    }
}

function toggleBookActive(id, activate, title) {
    const action = activate ? 'activate' : 'deactivate';
    UI.confirm(
        `${capitalize(action)} Book`,
        `Are you sure you want to ${action} "${title}"?`,
        async () => {
            try {
                if (activate) {
                    await API.post(`/api/books/${id}/activate`, {});
                } else {
                    await API.delete(`/api/books/${id}`);
                }
                UI.toast(`Book ${action}d successfully`, 'success');
                loadBooks();
            } catch (err) {
                UI.toast(err.message, 'error');
            }
        }
    );
}

// ======================== COPIES MANAGEMENT ========================

async function openCopiesModal(bookId, bookTitle) {
    currentBookForCopies = bookId;
    document.getElementById('copiesModalTitle').textContent = `Physical Copies: ${bookTitle}`;
    document.getElementById('copiesModalSubtitle').textContent = `Book ID #${bookId}`;
    document.getElementById('addCopyFormCard').classList.add('d-none');
    document.getElementById('newAccessionNumber').value = `ACC-${bookId}-${Math.floor(10 + Math.random() * 90)}`;
    copiesModalInstance.show();
    loadCopies(bookId);
}

async function loadCopies(bookId) {
    const tbody = document.getElementById('copiesTableBody');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Loading copies...</td></tr>';

    try {
        const resp = await API.get(`/api/books/${bookId}/copies`);
        if (!resp || !resp.success) return;

        const copies = resp.data;
        if (copies.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No physical copies found for this book.</td></tr>';
            return;
        }

        tbody.innerHTML = copies.map(c => `
            <tr>
                <td><code>${c.accessionNumber}</code></td>
                <td>${UI.statusBadge(c.status)}</td>
                <td>
                    <select class="form-select form-select-sm" style="width: 110px;" onchange="updateCopyCondition(${c.id}, this.value)" data-role="STAFF">
                        <option value="New" ${c.condition === 'New' ? 'selected' : ''}>New</option>
                        <option value="Good" ${c.condition === 'Good' ? 'selected' : ''}>Good</option>
                        <option value="Fair" ${c.condition === 'Fair' ? 'selected' : ''}>Fair</option>
                        <option value="Worn" ${c.condition === 'Worn' ? 'selected' : ''}>Worn</option>
                    </select>
                </td>
                <td><small class="text-muted">${UI.formatDate(c.createdAt)}</small></td>
                <td class="text-end">
                    ${c.status === 'AVAILABLE' ?
                        `<button class="btn btn-sm btn-outline-danger" title="Mark Damaged" onclick="markCopyStatus(${c.id}, 'DAMAGED')" data-role="STAFF">
                            <i class="bi bi-tools"></i>
                        </button>` :
                        (c.status === 'DAMAGED' ?
                        `<button class="btn btn-sm btn-outline-success" title="Mark Repaired (Available)" onclick="markCopyStatus(${c.id}, 'AVAILABLE')" data-role="STAFF">
                            <i class="bi bi-check-lg"></i>
                        </button>` : '')
                    }
                </td>
            </tr>
        `).join('');

        Auth.applyRoleRestrictions();

    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-3">Error loading copies: ${e.message}</td></tr>`;
    }
}

function toggleAddCopyForm() {
    const card = document.getElementById('addCopyFormCard');
    card.classList.toggle('d-none');
}

async function handleAddCopy(e) {
    e.preventDefault();
    if (!currentBookForCopies) return;

    const accessionNumber = document.getElementById('newAccessionNumber').value.trim();
    const condition = document.getElementById('newCopyCondition').value;

    try {
        await API.post(`/api/books/${currentBookForCopies}/copies`, { accessionNumber, condition });
        UI.toast('Physical copy added successfully', 'success');
        document.getElementById('addCopyForm').reset();
        document.getElementById('addCopyFormCard').classList.add('d-none');
        loadCopies(currentBookForCopies);
        loadBooks(); // refresh count
    } catch (err) {
        UI.toast(err.message, 'error');
    }
}

async function updateCopyCondition(copyId, condition) {
    try {
        await API.put(`/api/books/copies/${copyId}`, { condition });
        UI.toast('Copy condition updated', 'success');
    } catch (err) {
        UI.toast(err.message, 'error');
    }
}

async function markCopyStatus(copyId, status) {
    try {
        await API.put(`/api/books/copies/${copyId}`, { status });
        UI.toast(`Copy status changed to ${status}`, 'success');
        loadCopies(currentBookForCopies);
        loadBooks();
    } catch (err) {
        UI.toast(err.message, 'error');
    }
}

function escapeQuote(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'");
}

function capitalize(s) {
    return s.charAt(0).toUpperCase() + s.slice(1);
}
