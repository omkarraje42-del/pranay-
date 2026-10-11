/**
 * Dashboard Controller: Loads real-time statistics from PostgreSQL backend
 */
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
});

let borrowChartInstance = null;
let categoryChartInstance = null;

async function loadDashboard() {
    try {
        const resp = await API.get('/api/dashboard');
        if (!resp || !resp.success || !resp.data) return;

        const { stats, chart, categories, popularBooks, recentLoans } = resp.data;

        // Populate Summary Stats
        if (stats) {
            document.getElementById('statTitles').textContent = stats.totalTitles || 0;
            document.getElementById('statTotalCopies').textContent = stats.totalCopies || 0;
            document.getElementById('statAvailable').textContent = stats.availableCopies || 0;
            document.getElementById('statBorrowed').textContent = stats.borrowedCopies || 0;
            document.getElementById('statPendingRes').textContent = stats.pendingReservations || 0;
            document.getElementById('statOverdue').textContent = stats.overdueLoans || 0;

            document.getElementById('statMembers').textContent = stats.totalMembers || 0;
            document.getElementById('statActiveMembers').textContent = stats.activeMembers || 0;
            document.getElementById('statFines').textContent = UI.formatCurrency(stats.outstandingFines);
            document.getElementById('statIssuedToday').textContent = stats.issuedToday || 0;
            document.getElementById('statReturnedToday').textContent = stats.returnedToday || 0;
        }

        // Render Borrowing Trends Chart
        if (chart) {
            renderBorrowingChart(chart);
        }

        // Render Category Doughnut Chart
        if (categories) {
            renderCategoryChart(categories);
        }

        // Populate Popular Books
        renderPopularBooks(popularBooks || []);

        // Populate Recent Loans
        renderRecentLoans(recentLoans || []);

    } catch (err) {
        console.error('Error loading dashboard:', err);
        UI.toast('Failed to load dashboard metrics: ' + err.message, 'error');
    }
}

function renderBorrowingChart(chartData) {
    const ctx = document.getElementById('borrowingChart');
    if (!ctx) return;

    if (borrowChartInstance) {
        borrowChartInstance.destroy();
    }

    borrowChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: chartData.labels || [],
            datasets: [
                {
                    label: 'Books Issued',
                    data: chartData.issues || [],
                    backgroundColor: 'rgba(37, 99, 235, 0.7)',
                    borderColor: '#2563eb',
                    borderWidth: 1,
                    borderRadius: 4
                },
                {
                    label: 'Books Returned',
                    data: chartData.returns || [],
                    backgroundColor: 'rgba(16, 185, 129, 0.7)',
                    borderColor: '#10b981',
                    borderWidth: 1,
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { precision: 0 }
                }
            },
            plugins: {
                legend: { position: 'top' }
            }
        }
    });
}

function renderCategoryChart(categories) {
    const ctx = document.getElementById('categoryChart');
    if (!ctx) return;

    if (categoryChartInstance) {
        categoryChartInstance.destroy();
    }

    const labels = categories.map(c => c.category);
    const data = categories.map(c => c.bookCount);
    const colors = [
        '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6',
        '#ec4899', '#06b6d4', '#64748b'
    ];

    categoryChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors.slice(0, labels.length)
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
            }
        }
    });
}

function renderPopularBooks(books) {
    const tbody = document.getElementById('popularBooksBody');
    if (!tbody) return;

    if (books.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">No borrowing data recorded yet</td></tr>';
        return;
    }

    tbody.innerHTML = books.map(b => `
        <tr>
            <td>
                <div class="fw-semibold text-dark">${b.title}</div>
                <small class="text-muted">ISBN: ${b.isbn || 'N/A'}</small>
            </td>
            <td><span class="badge bg-light text-secondary border">${b.category || 'General'}</span></td>
            <td class="text-end fw-bold text-primary">${b.borrowCount}</td>
        </tr>
    `).join('');
}

function renderRecentLoans(loans) {
    const tbody = document.getElementById('recentLoansBody');
    if (!tbody) return;

    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">No recent loans found</td></tr>';
        return;
    }

    tbody.innerHTML = loans.map(l => `
        <tr>
            <td>
                <div class="fw-semibold">${l.bookTitle}</div>
                <small class="text-muted">${l.accessionNumber}</small>
            </td>
            <td>
                <div>${l.memberName}</div>
                <small class="text-muted">${l.membershipNumber}</small>
            </td>
            <td>${UI.formatDate(l.dueDate)}</td>
            <td>${UI.statusBadge(l.status)}</td>
        </tr>
    `).join('');
}
