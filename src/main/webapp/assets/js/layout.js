/**
 * Shared Layout Component: Dynamically mounts the enterprise sidebar and top navbar
 */
const Layout = {
    render(activePageTitle) {
        const sidebarEl = document.getElementById('sidebar-container');
        const headerEl = document.getElementById('header-container');

        if (sidebarEl) {
            sidebarEl.innerHTML = `
                <div class="sidebar">
                    <div class="sidebar-brand">
                        <i class="bi bi-book-half"></i>
                        <span>LibraryOS</span>
                    </div>

                    <div class="sidebar-nav">
                        <div class="nav-section-title">Overview</div>
                        <a href="dashboard.html" class="sidebar-link" data-page="dashboard">
                            <i class="bi bi-grid-1x2-fill"></i>
                            <span>Dashboard</span>
                        </a>

                        <div class="nav-section-title">Catalog & Inventory</div>
                        <a href="books.html" class="sidebar-link" data-page="books">
                            <i class="bi bi-journal-bookmark-fill"></i>
                            <span>Book Catalog</span>
                        </a>
                        <a href="categories.html" class="sidebar-link" data-page="categories">
                            <i class="bi bi-tags-fill"></i>
                            <span>Categories</span>
                        </a>
                        <a href="authors.html" class="sidebar-link" data-page="authors">
                            <i class="bi bi-people-fill"></i>
                            <span>Authors</span>
                        </a>
                        <a href="publishers.html" class="sidebar-link" data-page="publishers">
                            <i class="bi bi-building"></i>
                            <span>Publishers</span>
                        </a>

                        <div class="nav-section-title">Circulation</div>
                        <a href="issue-book.html" class="sidebar-link" data-page="issue-book" data-role="STAFF">
                            <i class="bi bi-box-arrow-up-right"></i>
                            <span>Issue Book</span>
                        </a>
                        <a href="loans.html" class="sidebar-link" data-page="loans">
                            <i class="bi bi-arrow-left-right"></i>
                            <span>Loan Transactions</span>
                        </a>
                        <a href="overdue.html" class="sidebar-link" data-page="overdue">
                            <i class="bi bi-exclamation-triangle-fill text-warning"></i>
                            <span>Overdue Books</span>
                        </a>
                        <a href="reservations.html" class="sidebar-link" data-page="reservations">
                            <i class="bi bi-clock-history"></i>
                            <span>Reservations</span>
                        </a>

                        <div class="nav-section-title">Patrons & Accounts</div>
                        <a href="members.html" class="sidebar-link" data-page="members" data-role="STAFF">
                            <i class="bi bi-person-badge-fill"></i>
                            <span>Members</span>
                        </a>
                        <a href="fines.html" class="sidebar-link" data-page="fines">
                            <i class="bi bi-cash-stack"></i>
                            <span>Fines & Payments</span>
                        </a>

                        <div class="nav-section-title" data-role="ADMIN">System & Reports</div>
                        <a href="reports.html" class="sidebar-link" data-page="reports" data-role="STAFF">
                            <i class="bi bi-bar-chart-line-fill"></i>
                            <span>Reports</span>
                        </a>
                        <a href="users.html" class="sidebar-link" data-page="users" data-role="ADMIN">
                            <i class="bi bi-shield-lock-fill"></i>
                            <span>Staff Accounts</span>
                        </a>
                        <a href="profile.html" class="sidebar-link" data-page="profile">
                            <i class="bi bi-person-circle"></i>
                            <span>My Profile</span>
                        </a>
                    </div>

                    <div class="sidebar-footer">
                        <div class="d-flex align-items-center justify-content-between">
                            <span class="small">v1.0.0 (J2EE)</span>
                            <span class="badge bg-success-subtle text-success small">Connected</span>
                        </div>
                    </div>
                </div>
            `;
        }

        if (headerEl) {
            headerEl.innerHTML = `
                <div class="top-navbar">
                    <div class="d-flex align-items-center gap-3">
                        <button class="btn btn-sm btn-light d-lg-none" id="mobileMenuBtn">
                            <i class="bi bi-list fs-5"></i>
                        </button>
                        <h5 class="mb-0 fw-bold text-dark">${activePageTitle || 'Library Management'}</h5>
                    </div>

                    <div class="d-flex align-items-center gap-3">
                        <div class="d-flex align-items-center gap-2">
                            <div class="text-end d-none d-sm-block">
                                <div class="fw-semibold small" id="navUserName">Loading...</div>
                                <span class="badge bg-primary-subtle text-primary" style="font-size: 0.7rem;" id="navUserRole">ROLE</span>
                            </div>
                            <div class="rounded-circle bg-light border p-2 d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                                <i class="bi bi-person-fill text-secondary"></i>
                            </div>
                        </div>

                        <button class="btn btn-outline-danger btn-sm d-flex align-items-center gap-1" id="btnLogout" title="Log Out">
                            <i class="bi bi-box-arrow-right"></i>
                            <span class="d-none d-md-inline">Logout</span>
                        </button>
                    </div>
                </div>
            `;
        }

        // Highlight active link
        const currentPath = window.location.pathname;
        document.querySelectorAll('.sidebar-link').forEach(link => {
            const page = link.getAttribute('data-page');
            if (currentPath.includes(page)) {
                link.classList.add('active');
            }
        });

        // Setup mobile menu
        const mobileBtn = document.getElementById('mobileMenuBtn');
        const sidebar = document.querySelector('.sidebar');
        if (mobileBtn && sidebar) {
            mobileBtn.onclick = () => {
                sidebar.classList.toggle('open');
            };
        }
    }
};

window.Layout = Layout;
