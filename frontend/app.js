// ===== API Base URLs =====
const PRODUCT_API = 'http://localhost:8081/api/products';
const ORDER_API   = 'http://localhost:8082/api/orders';
const PAYMENT_API = 'http://localhost:8083/api/payments';

// ===== Utility: Show Bootstrap Alert =====
function showAlert(containerId, message, type = 'success') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const icon = type === 'success' ? 'bi-check-circle-fill'
               : type === 'danger'  ? 'bi-exclamation-triangle-fill'
               : 'bi-info-circle-fill';

    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show d-flex align-items-center gap-2`;
    alert.setAttribute('role', 'alert');
    alert.innerHTML = `
        <i class="bi ${icon}"></i>
        <span>${message}</span>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    container.prepend(alert);

    // Auto-dismiss after 5s
    setTimeout(() => {
        if (alert && alert.parentNode) {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 300);
        }
    }, 5000);
}

// ===== Utility: Format currency =====
function formatPrice(price) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(price);
}

// ===== Utility: Format date =====
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

// ===== Utility: Status badge =====
function getStatusBadge(status) {
    const map = {
        'PENDING':             'bg-warning',
        'PAYMENT_PROCESSING':  'bg-info',
        'CONFIRMED':           'bg-success',
        'FAILED':              'bg-danger',
        'CANCELLED':           'bg-secondary',
    };
    const cls = map[status] || 'bg-secondary';
    return `<span class="badge ${cls}">${status}</span>`;
}

// =============================================================
//                    PRODUCTS PAGE
// =============================================================

// Load all products
async function loadProducts() {
    const grid = document.getElementById('productsGrid');
    if (!grid) return;

    grid.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading products...</p>
        </div>
    `;

    try {
        const res = await fetch(PRODUCT_API);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const products = await res.json();

        if (products.length === 0) {
            grid.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-box-seam"></i>
                    <p>No products yet. Add your first product above!</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = products.map((p, index) => {
            const stockPercent = Math.min(100, (p.stockQuantity / 50) * 100);
            const stockClass = p.stockQuantity > 20 ? 'high' : p.stockQuantity > 5 ? 'medium' : 'low';
            return `
                <div class="product-card" style="animation-delay: ${index * 0.05}s;">
                    <div class="product-card-header">
                        <i class="bi bi-box-seam"></i>
                    </div>
                    <div class="product-card-body">
                        <div class="product-category">${escapeHtml(p.category || 'General')}</div>
                        <h3 class="product-name">${escapeHtml(p.name)}</h3>
                        <p class="product-description">${escapeHtml(p.description || 'No description')}</p>
                        <div class="product-footer">
                            <div>
                                <div class="product-price">₹${p.price.toFixed(2)}</div>
                                <div class="product-stock">
                                    Stock: ${p.stockQuantity}
                                    <div class="stock-bar">
                                        <div class="stock-bar-fill ${stockClass}" style="width: ${stockPercent}%"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        grid.innerHTML = `
            <div class="empty-state">
                <i class="bi bi-wifi-off"></i>
                <p>Failed to load products. Is the product-service running?</p>
            </div>
        `;
        showAlert('alertContainer', `Failed to load products: ${err.message}`, 'danger');
    }
}

// Add product handler
async function handleAddProduct(e) {
    e.preventDefault();
    const form = e.target;

    // Bootstrap validation
    if (!form.checkValidity()) {
        e.stopPropagation();
        form.classList.add('was-validated');
        return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Adding...`;

    const payload = {
        name:          form.productName.value.trim(),
        description:   form.productDescription.value.trim(),
        price:         parseFloat(form.productPrice.value),
        stockQuantity: parseInt(form.productStock.value),
        category:      form.productCategory.value.trim()
    };

    try {
        const res = await fetch(PRODUCT_API, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || `HTTP ${res.status}`);
        }

        const product = await res.json();
        showAlert('alertContainer', `Product "<strong>${escapeHtml(product.name)}</strong>" added successfully!`, 'success');
        form.reset();
        form.classList.remove('was-validated');
        await loadProducts();
    } catch (err) {
        showAlert('alertContainer', `Failed to add product: ${err.message}`, 'danger');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// =============================================================
//                    ORDERS PAGE
// =============================================================

// Place order handler
async function handlePlaceOrder(e) {
    e.preventDefault();
    const form = e.target;

    if (!form.checkValidity()) {
        e.stopPropagation();
        form.classList.add('was-validated');
        return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Placing...`;

    const payload = {
        userId:         form.userId.value.trim(),
        productId:      parseInt(form.productId.value),
        quantity:        parseInt(form.orderQuantity.value),
        idempotencyKey: 'key-' + Date.now()
    };

    // Show the generated key
    const keyDisplay = document.getElementById('idempotencyKeyDisplay');
    if (keyDisplay) keyDisplay.textContent = payload.idempotencyKey;

    try {
        const res = await fetch(ORDER_API, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || `HTTP ${res.status}`);
        }

        const order = await res.json();
        showAlert('alertContainer', `Order <strong>#${order.id}</strong> placed! Status: ${getStatusBadge(order.status)}`, 'success');
        form.reset();
        form.classList.remove('was-validated');
        if (keyDisplay) keyDisplay.textContent = '-';
    } catch (err) {
        showAlert('alertContainer', `Failed to place order: ${err.message}`, 'danger');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// Check order status
async function checkOrder() {
    const input = document.getElementById('checkOrderId');
    const resultDiv = document.getElementById('orderResult');
    const orderId = input?.value?.trim();

    if (!orderId) {
        showAlert('alertContainer', 'Please enter an Order ID.', 'danger');
        return;
    }

    resultDiv.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading...</p>
        </div>
    `;

    try {
        const res = await fetch(`${ORDER_API}/${orderId}`);
        if (!res.ok) throw new Error(`Order not found (HTTP ${res.status})`);
        const order = await res.json();

        resultDiv.innerHTML = `
            <div class="order-detail-row">
                <span class="order-detail-label">Order ID</span>
                <span class="order-detail-value">#${order.id}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">User ID</span>
                <span class="order-detail-value">${escapeHtml(order.userId)}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Product ID</span>
                <span class="order-detail-value">${order.productId}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Quantity</span>
                <span class="order-detail-value">${order.quantity}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Total Amount</span>
                <span class="order-detail-value">${formatPrice(order.totalAmount)}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Status</span>
                <span class="order-detail-value">${getStatusBadge(order.status)}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Idempotency Key</span>
                <span class="order-detail-value" style="font-size:0.8rem;font-family:'Fira Code',monospace;">${escapeHtml(order.idempotencyKey)}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Created</span>
                <span class="order-detail-value">${formatDate(order.createdAt)}</span>
            </div>
            <div class="order-detail-row">
                <span class="order-detail-label">Updated</span>
                <span class="order-detail-value">${formatDate(order.updatedAt)}</span>
            </div>
        `;
    } catch (err) {
        resultDiv.innerHTML = `
            <div class="empty-state">
                <i class="bi bi-search"></i>
                <p>${err.message}</p>
            </div>
        `;
    }
}

// ===== Utility: Escape HTML =====
function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

// =============================================================
//                    INITIALIZATION
// =============================================================
document.addEventListener('DOMContentLoaded', () => {
    // Products page: load products & bind form
    const addProductForm = document.getElementById('addProductForm');
    if (addProductForm) {
        addProductForm.addEventListener('submit', handleAddProduct);
        loadProducts();
    }

    // Orders page: bind forms
    const placeOrderForm = document.getElementById('placeOrderForm');
    if (placeOrderForm) {
        placeOrderForm.addEventListener('submit', handlePlaceOrder);
    }

    const checkOrderBtn = document.getElementById('checkOrderBtn');
    if (checkOrderBtn) {
        checkOrderBtn.addEventListener('click', checkOrder);
    }

    // Allow pressing Enter on Order ID input
    const checkOrderInput = document.getElementById('checkOrderId');
    if (checkOrderInput) {
        checkOrderInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') checkOrder();
        });
    }
});
