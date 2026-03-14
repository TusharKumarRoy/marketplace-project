document.addEventListener('DOMContentLoaded', () => {
    const searchForms = document.querySelectorAll('[data-auto-trim-search]');
    searchForms.forEach((form) => {
        form.addEventListener('submit', () => {
            const input = form.querySelector('input[name="q"]');
            if (input) {
                input.value = input.value.trim();
            }
        });
    });

    const quantitySelectors = document.querySelectorAll('[data-quantity-selector]');
    quantitySelectors.forEach((wrapper) => {
        const input = wrapper.querySelector('input[type="number"]');
        const decreaseBtn = wrapper.querySelector('[data-qty="dec"]');
        const increaseBtn = wrapper.querySelector('[data-qty="inc"]');

        if (!input || !decreaseBtn || !increaseBtn) {
            return;
        }

        decreaseBtn.addEventListener('click', () => {
            const min = Number(input.min || 1);
            const current = Number(input.value || min);
            input.value = Math.max(min, current - 1);
        });

        increaseBtn.addEventListener('click', () => {
            const max = Number(input.max || 999);
            const current = Number(input.value || 1);
            input.value = Math.min(max, current + 1);
        });
    });

    const buyerOrdersRoot = document.querySelector('[data-buyer-orders-page="true"]');
    if (!buyerOrdersRoot) {
        return;
    }

    const statusClasses = ['status-pending', 'status-confirmed', 'status-processing', 'status-shipped', 'status-delivered', 'status-cancelled'];

    const renderCancelCell = (row, canCancel, orderId) => {
        const actionCell = row.cells[4];
        if (!actionCell) {
            return;
        }

        if (canCancel) {
            actionCell.innerHTML = `
                <form class="cancel-order-form" action="/buyer/orders/${orderId}/cancel" method="post">
                    <button type="submit" class="btn btn-small btn-secondary">Cancel</button>
                </form>
            `;
            return;
        }

        actionCell.innerHTML = '<span class="cancel-order-disabled">-</span>';
    };

    const updateStatuses = (items) => {
        const byId = new Map(items.map((item) => [String(item.orderId), item]));
        const rows = buyerOrdersRoot.querySelectorAll('tbody tr[data-order-id]');

        rows.forEach((row) => {
            const orderId = row.getAttribute('data-order-id');
            const data = byId.get(orderId);
            if (!data) {
                return;
            }

            const badge = row.querySelector('.order-status-badge');
            if (badge) {
                const statusText = String(data.status || '').toUpperCase();
                const statusClass = `status-${statusText.toLowerCase()}`;
                badge.textContent = statusText;
                statusClasses.forEach((c) => badge.classList.remove(c));
                badge.classList.add(statusClass);
            }

            renderCancelCell(row, Boolean(data.canCancel), orderId);
        });
    };

    const pollStatuses = async () => {
        try {
            const response = await fetch('/buyer/orders/statuses', { headers: { Accept: 'application/json' } });
            if (!response.ok) {
                return;
            }
            const payload = await response.json();
            if (Array.isArray(payload)) {
                updateStatuses(payload);
            }
        } catch (error) {
            // Keep polling silently; temporary network issues should not break UI behavior.
        }
    };

    pollStatuses();
    window.setInterval(pollStatuses, 5000);
});
