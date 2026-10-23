const API_BASE = "/api";

function getToken() {
    return localStorage.getItem("warehouse_token");
}

function getUser() {
    const raw = localStorage.getItem("warehouse_user");
    return raw ? JSON.parse(raw) : null;
}

function setAuth(token, user) {
    localStorage.setItem("warehouse_token", token);
    localStorage.setItem("warehouse_user", JSON.stringify(user));
}

function clearAuth() {
    localStorage.removeItem("warehouse_token");
    localStorage.removeItem("warehouse_user");
}

function logout() {
    clearAuth();
    window.location.href = "index.html";
}

async function authFetch(url, options = {}) {
    const token = getToken();
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }
    const response = await fetch(url, { ...options, headers });
    if (response.status === 401) {
        clearAuth();
        window.location.href = "index.html";
        throw new Error("Session expired. Please log in again.");
    }
    return response;
}

function showAlert(boxId, message, isError = false) {
    const box = document.getElementById(boxId);
    if (!box) return;
    box.textContent = message;
    box.className = "alert " + (isError ? "alert-error" : "alert-success");
    box.style.display = "block";
    setTimeout(() => {
        box.style.display = "none";
    }, 4500);
}

function setupHeaderNav() {
    const user = getUser();
    const userInfoEl = document.getElementById("header-user-info");
    const navManagementEl = document.getElementById("nav-management");

    if (user && userInfoEl) {
        const roleClass = user.role === "ADMIN" ? "user-role-admin" : "user-role-staff";
        const branchInfo = user.branchName ? " (" + user.branchName + ")" : "";
        userInfoEl.innerHTML = `
            <span class="user-badge">${escapeHtml(user.name)} <span class="${roleClass}">[${user.role}]</span>${escapeHtml(branchInfo)}</span>
            <button class="nav-btn btn-logout" onclick="logout()">Logout</button>
        `;
    }

    if (user && navManagementEl) {
        if (user.role !== "ADMIN") {
            navManagementEl.style.display = "none";
        }
    }
}

function requireAuth(allowedRoles = []) {
    const token = getToken();
    const user = getUser();
    if (!token || !user) {
        window.location.href = "index.html";
        return false;
    }
    if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
        alert("Access Denied: You do not have permission to view this page.");
        window.location.href = "dashboard.html";
        return false;
    }
    return true;
}

function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function setupTabs(containerSelector = ".tabs") {
    const container = document.querySelector(containerSelector);
    if (!container) return;
    const buttons = container.querySelectorAll(".tab-btn");
    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            buttons.forEach(b => b.classList.remove("active"));
            document.querySelectorAll(".tab-content").forEach(tc => tc.classList.remove("active"));
            btn.classList.add("active");
            const targetId = btn.getAttribute("data-tab");
            const targetEl = document.getElementById(targetId);
            if (targetEl) targetEl.classList.add("active");
        });
    });
}

function initIndexPage() {
    if (getToken() && getUser()) {
        window.location.href = "dashboard.html";
        return;
    }

    const tabLogin = document.getElementById("tab-login-btn");
    const tabRegister = document.getElementById("tab-register-btn");
    const formLogin = document.getElementById("login-form-section");
    const formRegister = document.getElementById("register-form-section");

    if (tabLogin && tabRegister) {
        tabLogin.addEventListener("click", () => {
            tabLogin.classList.add("active");
            tabRegister.classList.remove("active");
            formLogin.style.display = "block";
            formRegister.style.display = "none";
        });
        tabRegister.addEventListener("click", () => {
            tabRegister.classList.add("active");
            tabLogin.classList.remove("active");
            formRegister.style.display = "block";
            formLogin.style.display = "none";
            loadRegisterBranches();
        });
    }

    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = document.getElementById("login-email").value.trim();
            const password = document.getElementById("login-password").value;

            try {
                const res = await fetch(API_BASE + "/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, password })
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Invalid email or password");
                }
                setAuth(data.token, {
                    userId: data.userId,
                    name: data.name,
                    email: data.email,
                    role: data.role,
                    branchId: data.branchId,
                    branchName: data.branchName
                });
                window.location.href = "dashboard.html";
            } catch (err) {
                showAlert("auth-alert", err.message, true);
            }
        });
    }

    const registerForm = document.getElementById("register-form");
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const name = document.getElementById("reg-name").value.trim();
            const email = document.getElementById("reg-email").value.trim();
            const password = document.getElementById("reg-password").value;
            const role = "STAFF";

            try {
                const res = await fetch(API_BASE + "/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ name, email, password, role })
                });
                const data = await res.json();
                if (!res.ok) {
                    throw new Error(data.message || "Registration failed");
                }
                setAuth(data.token, {
                    userId: data.userId,
                    name: data.name,
                    email: data.email,
                    role: data.role,
                    branchId: data.branchId,
                    branchName: data.branchName
                });
                window.location.href = "dashboard.html";
            } catch (err) {
                showAlert("auth-alert", err.message, true);
            }
        });
    }
}

async function loadRegisterBranches() {
    const branchSelect = document.getElementById("reg-branch");
    if (!branchSelect) return;
    try {
        const res = await fetch(API_BASE + "/management/branches");
        if (res.ok) {
            const list = await res.json();
            branchSelect.innerHTML = `<option value="">-- None / Head Office --</option>` +
                list.map(b => `<option value="${b.id}">${escapeHtml(b.name)} (${escapeHtml(b.location)})</option>`).join("");
        }
    } catch (ignored) {}
}

function renderMovementChart(entries) {
    const chartEl = document.getElementById("movement-chart");
    if (!chartEl) return;

    if (!entries || entries.length === 0) {
        chartEl.innerHTML = '<div class="empty-state">No stock movement data is available yet.</div>';
        return;
    }

    const movementByDate = new Map();
    entries.forEach(item => {
        const date = typeof item.createdAt === "string" ? item.createdAt.slice(0, 10) : "";
        if (!date) return;
        const quantity = Number(item.quantityChange || 0);
        const current = movementByDate.get(date) || 0;
        movementByDate.set(date, current + (Number.isFinite(quantity) ? quantity : 0));
    });

    const days = [...movementByDate.entries()]
        .sort(([dateA], [dateB]) => dateB.localeCompare(dateA))
        .slice(0, 7)
        .reverse();
    if (days.length === 0) {
        chartEl.innerHTML = '<div class="empty-state">Movement records do not include dates to chart.</div>';
        return;
    }

    const maxMagnitude = Math.max(...days.map(([, quantity]) => Math.abs(quantity)), 1);
    const dateFormatter = new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" });
    chartEl.innerHTML = days.map(([date, quantity]) => {
        const dateLabel = dateFormatter.format(new Date(`${date}T12:00:00`));
        const height = Math.max(3, (Math.abs(quantity) / maxMagnitude) * 46);
        const sign = quantity > 0 ? "+" : "";
        const direction = quantity < 0 ? "is-negative" : "is-positive";
        return `
            <div class="movement-day" title="${escapeHtml(date)}: ${sign}${quantity} units">
                <span class="movement-value">${sign}${quantity}</span>
                <div class="movement-plot">
                    <div class="movement-zero-line"></div>
                    <div class="movement-bar ${direction}" style="--movement-size: ${height}%"></div>
                </div>
                <span class="movement-date">${escapeHtml(dateLabel)}</span>
            </div>
        `;
    }).join("");
}

function renderBranchInventory(items) {
    const inventoryEl = document.getElementById("branch-inventory");
    if (!inventoryEl) return;

    if (!items || items.length === 0) {
        inventoryEl.innerHTML = '<div class="empty-state">No stock records are available yet.</div>';
        return;
    }

    const inventoryByBranch = new Map();
    items.forEach(item => {
        const branchName = item.branchName || "Unassigned";
        const branch = inventoryByBranch.get(branchName) || { units: 0, records: 0, lowStock: 0 };
        const quantity = Number(item.quantity || 0);
        const threshold = Number(item.minimumStockThreshold || 0);
        branch.units += Number.isFinite(quantity) ? quantity : 0;
        branch.records += 1;
        if (item.lowStock || quantity <= threshold) branch.lowStock += 1;
        inventoryByBranch.set(branchName, branch);
    });

    const branches = [...inventoryByBranch.entries()].sort(([, branchA], [, branchB]) => branchB.units - branchA.units);
    const maxUnits = Math.max(...branches.map(([, branch]) => branch.units), 1);
    inventoryEl.innerHTML = branches.map(([branchName, branch]) => `
        <div class="branch-row">
            <span class="branch-name" title="${escapeHtml(branchName)}">${escapeHtml(branchName)}</span>
            <span class="branch-units">${branch.units.toLocaleString()} units</span>
            <div class="branch-meter" aria-label="${Math.round((branch.units / maxUnits) * 100)}% of highest branch stock">
                <div class="branch-meter-fill" style="width: ${Math.max(2, (branch.units / maxUnits) * 100)}%"></div>
            </div>
            <span class="branch-meta">${branch.records} stock records · ${branch.lowStock} low-stock</span>
        </div>
    `).join("");
}

function renderActivityFeed(entries) {
    const feedEl = document.getElementById("activity-feed");
    if (!feedEl) return;

    if (!entries || entries.length === 0) {
        feedEl.innerHTML = '<div class="empty-state">No recent stock movements available.</div>';
        return;
    }

    const visibleEntries = [...entries]
        .sort((itemA, itemB) => String(itemB.createdAt || "").localeCompare(String(itemA.createdAt || "")))
        .slice(0, 5);
    feedEl.innerHTML = visibleEntries.map(item => {
        const delta = Number(item.quantityChange || 0);
        const isPositive = delta > 0;
        const badgeClass = isPositive ? "activity-amount" : "activity-amount negative";
        const label = item.movementType || "Movement";
        const amount = `${isPositive ? "+" : ""}${delta}`;
        const createdAt = item.createdAt ? new Date(item.createdAt).toLocaleString() : "";
        return `
            <div class="activity-item">
                <div class="activity-icon">${isPositive ? "↗" : "↘"}</div>
                <div class="activity-content">
                    <strong>${escapeHtml(item.productName || "Inventory item")}</strong>
                    <span>${escapeHtml(item.branchName || "Warehouse")} · ${escapeHtml(label)}${createdAt ? ` · ${escapeHtml(createdAt)}` : ""}</span>
                </div>
                <div class="${badgeClass}">${amount}</div>
            </div>
        `;
    }).join("");
}

async function initDashboardPage() {
    if (!requireAuth()) return;
    setupHeaderNav();

    try {
        const summaryRes = await authFetch(API_BASE + "/dashboard/summary");
        if (!summaryRes.ok) throw new Error("Failed to load dashboard data");
        const data = await summaryRes.json();

        document.getElementById("stat-branches").textContent = data.totalBranches;
        document.getElementById("stat-products").textContent = data.totalProducts;
        document.getElementById("stat-low-stock").textContent = data.lowStockCount;
        document.getElementById("stat-transfers").textContent = data.pendingTransfersCount;
        document.getElementById("stat-purchases").textContent = data.pendingPurchasesCount;

        const tableBody = document.getElementById("low-stock-table-body");
        if (tableBody) {
            if (!data.lowStockItems || data.lowStockItems.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-muted" style="padding: 1.5rem;">All products have healthy stock levels. No low stock alerts!</td></tr>`;
            } else {
                tableBody.innerHTML = data.lowStockItems.map(item => `
                    <tr>
                        <td><strong>${escapeHtml(item.productSku)}</strong></td>
                        <td>${escapeHtml(item.productName)}</td>
                        <td>${escapeHtml(item.branchName)}</td>
                        <td style="color: #b91c1c; font-weight: 800;">${item.quantity}</td>
                        <td>${item.minimumStockThreshold}</td>
                        <td><span class="badge badge-low-stock">LOW STOCK</span></td>
                    </tr>
                `).join("");
            }
        }

    } catch (err) {
        showAlert("dashboard-alert", err.message, true);
    }

    const [movements, stocks] = await Promise.all([
        authFetch(API_BASE + "/operations/stock-movements")
            .then(res => res.ok ? res.json() : [])
            .catch(() => []),
        authFetch(API_BASE + "/operations/stocks")
            .then(res => res.ok ? res.json() : [])
            .catch(() => [])
    ]);
    renderMovementChart(movements || []);
    renderActivityFeed(movements || []);
    renderBranchInventory(stocks || []);
}

let cachedBranches = [];
let cachedProducts = [];

async function initManagementPage() {
    if (!requireAuth(["ADMIN"])) return;
    setupHeaderNav();
    setupTabs();

    await loadBranches();
    await loadProducts();
    await loadStaff();

    const branchForm = document.getElementById("branch-form");
    if (branchForm) {
        branchForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("branch-id").value;
            const name = document.getElementById("branch-name").value.trim();
            const location = document.getElementById("branch-location").value.trim();

            const isEdit = !!id;
            const url = isEdit ? `${API_BASE}/management/branches/${id}` : `${API_BASE}/management/branches`;
            const method = isEdit ? "PUT" : "POST";

            try {
                const res = await authFetch(url, {
                    method,
                    body: JSON.stringify({ name, location })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to save branch");

                showAlert("mgmt-alert", `Branch "${data.name}" saved successfully!`);
                resetBranchForm();
                await loadBranches();
            } catch (err) {
                showAlert("mgmt-alert", err.message, true);
            }
        });
    }

    const productForm = document.getElementById("product-form");
    if (productForm) {
        productForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("product-id").value;
            const sku = document.getElementById("product-sku").value.trim();
            const name = document.getElementById("product-name").value.trim();
            const description = document.getElementById("product-description").value.trim();
            const minimumStockThreshold = parseInt(document.getElementById("product-min-stock").value);

            const isEdit = !!id;
            const url = isEdit ? `${API_BASE}/management/products/${id}` : `${API_BASE}/management/products`;
            const method = isEdit ? "PUT" : "POST";

            try {
                const res = await authFetch(url, {
                    method,
                    body: JSON.stringify({ sku, name, description, minimumStockThreshold })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to save product");

                showAlert("mgmt-alert", `Product "${data.name}" saved successfully!`);
                resetProductForm();
                await loadProducts();
            } catch (err) {
                showAlert("mgmt-alert", err.message, true);
            }
        });
    }

    const staffForm = document.getElementById("staff-form");
    if (staffForm) {
        staffForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = document.getElementById("staff-id").value;
            const name = document.getElementById("staff-name").value.trim();
            const email = document.getElementById("staff-email").value.trim();
            const password = document.getElementById("staff-password").value;
            const role = document.getElementById("staff-role").value;
            const branchIdVal = document.getElementById("staff-branch").value;
            const assignedBranchId = branchIdVal ? parseInt(branchIdVal) : null;

            const isEdit = !!id;
            const url = isEdit ? `${API_BASE}/management/staff/${id}` : `${API_BASE}/management/staff`;
            const method = isEdit ? "PUT" : "POST";
            const payload = isEdit
                ? { name, email, role, assignedBranchId }
                : { name, email, password, role, assignedBranchId };

            try {
                const res = await authFetch(url, {
                    method,
                    body: JSON.stringify(payload)
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to save staff");

                showAlert("mgmt-alert", `Staff member "${data.name}" saved successfully!`);
                resetStaffForm();
                await loadStaff();
            } catch (err) {
                showAlert("mgmt-alert", err.message, true);
            }
        });
    }
}

async function loadBranches() {
    try {
        const res = await authFetch(`${API_BASE}/management/branches`);
        if (!res.ok) throw new Error("Failed to load branches");
        cachedBranches = await res.json();

        const tbody = document.getElementById("branches-table-body");
        if (tbody) {
            tbody.innerHTML = cachedBranches.map(b => `
                <tr>
                    <td>${b.id}</td>
                    <td><strong>${escapeHtml(b.name)}</strong></td>
                    <td>${escapeHtml(b.location)}</td>
                    <td>
                        <button class="btn btn-sm btn-secondary" onclick="editBranch(${b.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteBranch(${b.id})">Delete</button>
                    </td>
                </tr>
            `).join("");
        }

        populateBranchSelects();
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

function populateBranchSelects() {
    const staffBranchSelect = document.getElementById("staff-branch");
    if (staffBranchSelect) {
        staffBranchSelect.innerHTML = `<option value="">-- None / Head Office --</option>` +
            cachedBranches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
    }
}

function editBranch(id) {
    const branch = cachedBranches.find(b => b.id === id);
    if (!branch) return;
    document.getElementById("branch-id").value = branch.id;
    document.getElementById("branch-name").value = branch.name;
    document.getElementById("branch-location").value = branch.location;
    document.getElementById("branch-submit-btn").textContent = "Update Branch";
    document.getElementById("branch-cancel-btn").style.display = "inline-flex";
}

function resetBranchForm() {
    document.getElementById("branch-id").value = "";
    document.getElementById("branch-name").value = "";
    document.getElementById("branch-location").value = "";
    document.getElementById("branch-submit-btn").textContent = "Add Branch";
    document.getElementById("branch-cancel-btn").style.display = "none";
}

async function deleteBranch(id) {
    if (!confirm("Are you sure you want to delete this branch?")) return;
    try {
        const res = await authFetch(`${API_BASE}/management/branches/${id}`, { method: "DELETE" });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to delete branch");
        showAlert("mgmt-alert", "Branch deleted successfully.");
        await loadBranches();
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

async function loadProducts() {
    try {
        const res = await authFetch(`${API_BASE}/management/products`);
        if (!res.ok) throw new Error("Failed to load products");
        cachedProducts = await res.json();

        const tbody = document.getElementById("products-table-body");
        if (tbody) {
            tbody.innerHTML = cachedProducts.map(p => `
                <tr>
                    <td><strong>${escapeHtml(p.sku)}</strong></td>
                    <td>${escapeHtml(p.name)}</td>
                    <td>${escapeHtml(p.description || "-")}</td>
                    <td>${p.minimumStockThreshold}</td>
                    <td>
                        <button class="btn btn-sm btn-secondary" onclick="editProduct(${p.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProduct(${p.id})">Delete</button>
                    </td>
                </tr>
            `).join("");
        }
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

function editProduct(id) {
    const p = cachedProducts.find(item => item.id === id);
    if (!p) return;
    document.getElementById("product-id").value = p.id;
    document.getElementById("product-sku").value = p.sku;
    document.getElementById("product-name").value = p.name;
    document.getElementById("product-description").value = p.description || "";
    document.getElementById("product-min-stock").value = p.minimumStockThreshold;
    document.getElementById("product-submit-btn").textContent = "Update Product";
    document.getElementById("product-cancel-btn").style.display = "inline-flex";
}

function resetProductForm() {
    document.getElementById("product-id").value = "";
    document.getElementById("product-sku").value = "";
    document.getElementById("product-name").value = "";
    document.getElementById("product-description").value = "";
    document.getElementById("product-min-stock").value = "10";
    document.getElementById("product-submit-btn").textContent = "Add Product";
    document.getElementById("product-cancel-btn").style.display = "none";
}

async function deleteProduct(id) {
    if (!confirm("Are you sure you want to delete this product?")) return;
    try {
        const res = await authFetch(`${API_BASE}/management/products/${id}`, { method: "DELETE" });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to delete product");
        showAlert("mgmt-alert", "Product deleted successfully.");
        await loadProducts();
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

let cachedStaff = [];

async function loadStaff() {
    try {
        const res = await authFetch(`${API_BASE}/management/staff`);
        if (!res.ok) throw new Error("Failed to load staff");
        cachedStaff = await res.json();

        const tbody = document.getElementById("staff-table-body");
        if (tbody) {
            tbody.innerHTML = cachedStaff.map(s => `
                <tr>
                    <td>${s.id}</td>
                    <td><strong>${escapeHtml(s.name)}</strong></td>
                    <td>${escapeHtml(s.email)}</td>
                    <td><span class="badge ${s.role === 'ADMIN' ? 'badge-ok-stock' : 'badge-pending'}">${s.role}</span></td>
                    <td>${escapeHtml(s.assignedBranchName || "Not Assigned")}</td>
                    <td>
                        <button class="btn btn-sm btn-secondary" onclick="editStaff(${s.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteStaff(${s.id})">Delete</button>
                    </td>
                </tr>
            `).join("");
        }
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

function editStaff(id) {
    const s = cachedStaff.find(item => item.id === id);
    if (!s) return;
    document.getElementById("staff-id").value = s.id;
    document.getElementById("staff-name").value = s.name;
    document.getElementById("staff-email").value = s.email;
    document.getElementById("staff-email").disabled = true;
    document.getElementById("staff-password-group").style.display = "none";
    document.getElementById("staff-password").required = false;
    document.getElementById("staff-role").value = s.role;
    document.getElementById("staff-branch").value = s.assignedBranchId || "";
    document.getElementById("staff-submit-btn").textContent = "Update Staff";
    document.getElementById("staff-cancel-btn").style.display = "inline-flex";
}

function resetStaffForm() {
    document.getElementById("staff-id").value = "";
    document.getElementById("staff-name").value = "";
    document.getElementById("staff-email").value = "";
    document.getElementById("staff-email").disabled = false;
    document.getElementById("staff-password-group").style.display = "flex";
    document.getElementById("staff-password").required = true;
    document.getElementById("staff-password").value = "";
    document.getElementById("staff-role").value = "STAFF";
    document.getElementById("staff-branch").value = "";
    document.getElementById("staff-submit-btn").textContent = "Add Staff";
    document.getElementById("staff-cancel-btn").style.display = "none";
}

async function deleteStaff(id) {
    if (!confirm("Are you sure you want to delete this staff member?")) return;
    try {
        const res = await authFetch(`${API_BASE}/management/staff/${id}`, { method: "DELETE" });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to delete staff");
        showAlert("mgmt-alert", "Staff member deleted successfully.");
        await loadStaff();
    } catch (err) {
        showAlert("mgmt-alert", err.message, true);
    }
}

async function initOperationsPage() {
    if (!requireAuth()) return;
    setupHeaderNav();
    setupTabs();

    const currentUser = getUser();
    if (currentUser && currentUser.role !== "ADMIN") {
        const stockAddButton = document.querySelector('button[onclick="openStockModal()"]');
        if (stockAddButton) stockAddButton.textContent = "+ Adjust Own Branch Stock";

        const transferHeading = document.getElementById("transfer-form-heading");
        if (transferHeading) transferHeading.textContent = "Request Stock from Another Branch";

        const transferHelp = document.getElementById("transfer-request-help");
        if (transferHelp) transferHelp.textContent = "Choose the branch sending stock. Your assigned branch is automatically set as the receiving destination.";
    }

    await loadBranchesForOperations();
    await loadProductsForOperations();
    await loadStocks();
    await loadTransfers();
    await loadPurchases();
    await loadStockMovementHistory();

    const stockFilter = document.getElementById("stock-branch-filter");
    if (stockFilter) {
        stockFilter.addEventListener("change", () => loadStocks());
    }

    const transferFilter = document.getElementById("transfer-branch-filter");
    if (transferFilter) {
        transferFilter.addEventListener("change", () => loadTransfers());
    }

    const purchaseFilter = document.getElementById("purchase-branch-filter");
    if (purchaseFilter) {
        purchaseFilter.addEventListener("change", () => loadPurchases());
    }

    const historyFilter = document.getElementById("history-branch-filter");
    if (historyFilter) {
        historyFilter.addEventListener("change", () => loadStockMovementHistory());
    }

    const exportButton = document.getElementById("export-inventory-btn");
    if (exportButton) {
        exportButton.addEventListener("click", async () => {
            const currentUser = getUser();
            const stockFilter = document.getElementById("stock-branch-filter");
            let url = `${API_BASE}/exports/inventory`;
            let branchId = currentUser?.role === "ADMIN"
                ? (stockFilter && stockFilter.value ? stockFilter.value : "")
                : (currentUser?.branchId || "");

            if (branchId) url += `?branchId=${encodeURIComponent(branchId)}`;

            try {
                const res = await authFetch(url);
                if (!res.ok) {
                    let message = "Failed to export inventory";
                    try {
                        const data = await res.json();
                        message = data.message || message;
                    } catch (ignored) {}
                    throw new Error(message);
                }

                const blob = await res.blob();
                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement("a");
                link.href = downloadUrl;
                link.download = "warehouse_inventory.xlsx";
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(downloadUrl);
                showAlert("ops-alert", "Inventory Excel file downloaded successfully.");
            } catch (err) {
                showAlert("ops-alert", err.message, true);
            }
        });
    }

    const stockForm = document.getElementById("stock-update-form");
    if (stockForm) {
        stockForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const branchId = parseInt(document.getElementById("stock-modal-branch").value);
            const productId = parseInt(document.getElementById("stock-modal-product").value);
            const quantity = parseInt(document.getElementById("stock-modal-quantity").value);

            try {
                const res = await authFetch(`${API_BASE}/operations/stocks`, {
                    method: "POST",
                    body: JSON.stringify({ branchId, productId, quantity })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to update stock");

                showAlert("ops-alert", `Stock updated successfully for ${data.productName} (${data.branchName}): ${data.quantity} units.`);
                closeStockModal();
                await loadStocks();
            } catch (err) {
                showAlert("ops-alert", err.message, true);
            }
        });
    }

    const transferForm = document.getElementById("transfer-create-form");
    if (transferForm) {
        transferForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const sourceBranchId = parseInt(document.getElementById("tr-source-branch").value);
            const destinationBranchId = parseInt(document.getElementById("tr-dest-branch").value);
            const productId = parseInt(document.getElementById("tr-product").value);
            const quantity = parseInt(document.getElementById("tr-quantity").value);

            try {
                const res = await authFetch(`${API_BASE}/operations/transfers`, {
                    method: "POST",
                    body: JSON.stringify({ sourceBranchId, destinationBranchId, productId, quantity })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to create transfer request");

                showAlert("ops-alert", `Transfer request created successfully! Status: ${data.status}`);
                transferForm.reset();
                await loadTransfers();
            } catch (err) {
                showAlert("ops-alert", err.message, true);
            }
        });
    }

    const purchaseForm = document.getElementById("purchase-create-form");
    if (purchaseForm) {
        purchaseForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const branchId = parseInt(document.getElementById("po-branch").value);
            const productId = parseInt(document.getElementById("po-product").value);
            const requestedQuantity = parseInt(document.getElementById("po-quantity").value);

            try {
                const res = await authFetch(`${API_BASE}/operations/purchases`, {
                    method: "POST",
                    body: JSON.stringify({ branchId, productId, requestedQuantity })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.message || "Failed to create purchase request");

                showAlert("ops-alert", `Purchase replenishment request created! Status: ${data.status}`);
                purchaseForm.reset();
                await loadPurchases();
            } catch (err) {
                showAlert("ops-alert", err.message, true);
            }
        });
    }
}

async function loadBranchesForOperations() {
    try {
        const res = await authFetch(`${API_BASE}/management/branches`);
        if (res.ok) {
            cachedBranches = await res.json();
            const currentUser = getUser();
            const isAdmin = currentUser && currentUser.role === "ADMIN";
            const allOptions = cachedBranches.map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`).join("");
            const ownBranch = cachedBranches.find(b => currentUser && Number(b.id) === Number(currentUser.branchId));
            const ownOption = ownBranch ? `<option value="${ownBranch.id}">${escapeHtml(ownBranch.name)}</option>` : "";

            const stockFilter = document.getElementById("stock-branch-filter");
            if (stockFilter) stockFilter.innerHTML = `<option value="">All Branches</option>` + allOptions;

            const trFilter = document.getElementById("transfer-branch-filter");
            if (trFilter) {
                trFilter.innerHTML = isAdmin ? `<option value="">All Branches</option>` + allOptions : ownOption;
                if (!isAdmin) {
                    trFilter.value = currentUser.branchId || "";
                    trFilter.disabled = true;
                }
            }

            const poFilter = document.getElementById("purchase-branch-filter");
            if (poFilter) {
                poFilter.innerHTML = isAdmin ? `<option value="">All Branches</option>` + allOptions : ownOption;
                if (!isAdmin) {
                    poFilter.value = currentUser.branchId || "";
                    poFilter.disabled = true;
                }
            }

            const historyFilter = document.getElementById("history-branch-filter");
            if (historyFilter) {
                historyFilter.innerHTML = isAdmin ? `<option value="">All Branches</option>` + allOptions : ownOption;
                if (!isAdmin) {
                    historyFilter.value = currentUser.branchId || "";
                    historyFilter.disabled = true;
                }
            }

            const trSource = document.getElementById("tr-source-branch");
            if (trSource) {
                const sourceOptions = isAdmin
                    ? allOptions
                    : cachedBranches
                        .filter(b => Number(b.id) !== Number(currentUser?.branchId))
                        .map(b => `<option value="${b.id}">${escapeHtml(b.name)}</option>`)
                        .join("");
                trSource.innerHTML = sourceOptions || `<option value="">No other branches available</option>`;
            }

            const trDest = document.getElementById("tr-dest-branch");
            if (trDest) {
                trDest.innerHTML = isAdmin ? allOptions : ownOption;
                trDest.disabled = !isAdmin;
            }

            const poBranch = document.getElementById("po-branch");
            if (poBranch) poBranch.innerHTML = isAdmin ? allOptions : ownOption;

            const modalBranch = document.getElementById("stock-modal-branch");
            if (modalBranch) modalBranch.innerHTML = isAdmin ? allOptions : ownOption;
        }
    } catch (ignored) {}
}

async function loadProductsForOperations() {
    try {
        const res = await authFetch(`${API_BASE}/management/products`);
        if (res.ok) {
            cachedProducts = await res.json();
            const options = cachedProducts.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.sku)})</option>`).join("");

            const trProd = document.getElementById("tr-product");
            if (trProd) trProd.innerHTML = options;

            const poProd = document.getElementById("po-product");
            if (poProd) poProd.innerHTML = options;

            const modalProd = document.getElementById("stock-modal-product");
            if (modalProd) modalProd.innerHTML = options;
        }
    } catch (ignored) {}
}

async function loadStocks() {
    const filterEl = document.getElementById("stock-branch-filter");
    const branchId = filterEl && filterEl.value ? filterEl.value : "";
    const url = branchId ? `${API_BASE}/operations/stocks?branchId=${branchId}` : `${API_BASE}/operations/stocks`;

    try {
        const res = await authFetch(url);
        if (!res.ok) throw new Error("Failed to load stocks");
        const list = await res.json();

        const tbody = document.getElementById("stock-table-body");
        if (tbody) {
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: #64748b; padding: 1.5rem;">No stock records found. Use the 'Adjust / Add Stock' button to add stock.</td></tr>`;
            } else {
                tbody.innerHTML = list.map(item => `
                    <tr>
                        <td><strong>${escapeHtml(item.productSku)}</strong></td>
                        <td>${escapeHtml(item.productName)}</td>
                        <td>${escapeHtml(item.branchName)}</td>
                        <td style="font-weight: bold; ${item.lowStock ? 'color: #b91c1c;' : 'color: #1e293b;'}">${item.quantity}</td>
                        <td>${item.minimumStockThreshold}</td>
                        <td>
                            <span class="badge ${item.lowStock ? 'badge-low-stock' : 'badge-ok-stock'}">
                                ${item.lowStock ? 'LOW STOCK' : 'HEALTHY'}
                            </span>
                        </td>
                        <td>
                            ${getUser()?.role === "ADMIN" || Number(getUser()?.branchId) === Number(item.branchId) ? `<button class="btn btn-sm btn-secondary" onclick="openStockModalFor(${item.branchId}, ${item.productId}, ${item.quantity})">Adjust</button>` : `<span style="color: #64748b; font-size: 0.8rem;">View only</span>`}
                        </td>
                    </tr>
                `).join("");
            }
        }
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}

function openStockModal() {
    const modal = document.getElementById("stock-modal");
    if (modal) {
        const currentUser = getUser();
        if (currentUser && currentUser.role !== "ADMIN") {
            document.getElementById("stock-modal-branch").value = currentUser.branchId || "";
        }
        document.getElementById("stock-modal-quantity").value = "0";
        modal.classList.add("active");
    }
}

function openStockModalFor(branchId, productId, currentQty) {
    const modal = document.getElementById("stock-modal");
    if (modal) {
        document.getElementById("stock-modal-branch").value = branchId;
        document.getElementById("stock-modal-product").value = productId;
        document.getElementById("stock-modal-quantity").value = currentQty;
        modal.classList.add("active");
    }
}

function closeStockModal() {
    const modal = document.getElementById("stock-modal");
    if (modal) modal.classList.remove("active");
}

async function loadTransfers() {
    const filterEl = document.getElementById("transfer-branch-filter");
    const branchId = filterEl && filterEl.value ? filterEl.value : "";
    const url = branchId ? `${API_BASE}/operations/transfers?branchId=${branchId}` : `${API_BASE}/operations/transfers`;

    try {
        const res = await authFetch(url);
        if (!res.ok) throw new Error("Failed to load transfer requests");
        const list = await res.json();

        const tbody = document.getElementById("transfers-table-body");
        if (tbody) {
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: #64748b; padding: 1.5rem;">No transfer requests found.</td></tr>`;
            } else {
                tbody.innerHTML = list.map(item => {
                    let badgeClass = "badge-pending";
                    if (item.status === "APPROVED") badgeClass = "badge-approved";
                    else if (item.status === "COMPLETED") badgeClass = "badge-completed";
                    else if (item.status === "REJECTED") badgeClass = "badge-rejected";

                    let actionButtons = "";
                    if (getUser()?.role !== "ADMIN") {
                        actionButtons = `<span style="color: #64748b; font-size: 0.8rem;">Admin approval required</span>`;
                    } else if (item.status === "PENDING") {
                        actionButtons = `
                            <button class="btn btn-sm btn-success" onclick="updateTransferStatus(${item.id}, 'APPROVE')">Approve</button>
                            <button class="btn btn-sm btn-danger" onclick="updateTransferStatus(${item.id}, 'REJECT')">Reject</button>
                        `;
                    } else if (item.status === "APPROVED") {
                        actionButtons = `
                            <button class="btn btn-sm btn-primary" onclick="updateTransferStatus(${item.id}, 'COMPLETE')">Complete Receipt</button>
                            <button class="btn btn-sm btn-danger" onclick="updateTransferStatus(${item.id}, 'REJECT')">Cancel</button>
                        `;
                    } else {
                        actionButtons = `<span style="color: #64748b; font-size: 0.8rem;">Finished</span>`;
                    }

                    return `
                        <tr>
                            <td>#${item.id}</td>
                            <td>${escapeHtml(item.sourceBranchName)}</td>
                            <td>${escapeHtml(item.destinationBranchName)}</td>
                            <td>${escapeHtml(item.productName)} (<strong>${escapeHtml(item.productSku)}</strong>)</td>
                            <td style="font-weight: bold;">${item.quantity}</td>
                            <td><span class="badge ${badgeClass}">${item.status}</span></td>
                            <td>${escapeHtml(item.requestedByName || "System")}</td>
                            <td>${actionButtons}</td>
                        </tr>
                    `;
                }).join("");
            }
        }
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}

async function loadStockMovementHistory() {
    const filterEl = document.getElementById("history-branch-filter");
    const branchId = filterEl && filterEl.value ? filterEl.value : "";
    const url = branchId
        ? `${API_BASE}/operations/stock-movements?branchId=${encodeURIComponent(branchId)}`
        : `${API_BASE}/operations/stock-movements`;

    try {
        const res = await authFetch(url);
        if (!res.ok) {
            let message = "Failed to load stock movement history";
            try {
                const data = await res.json();
                message = data.message || message;
            } catch (ignored) {}
            throw new Error(message);
        }

        const list = await res.json();
        const tbody = document.getElementById("history-table-body");
        if (!tbody) return;

        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: #64748b; padding: 1.5rem;">No stock movements recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map(item => {
            const change = Number(item.quantityChange || 0);
            const changeText = `${change > 0 ? "+" : ""}${change}`;
            const changeColor = change > 0 ? "#15803d" : change < 0 ? "#b91c1c" : "#64748b";
            const dateText = item.createdAt ? item.createdAt.replace("T", " ") : "-";
            const referenceText = item.referenceId ? `Transfer #${item.referenceId}` : "-";

            return `
                <tr>
                    <td>${escapeHtml(dateText)}</td>
                    <td>${escapeHtml(item.productName)} (<strong>${escapeHtml(item.productSku)}</strong>)</td>
                    <td>${escapeHtml(item.branchName)}</td>
                    <td>${escapeHtml(item.movementType)}</td>
                    <td style="font-weight: bold; color: ${changeColor};">${changeText}</td>
                    <td>${item.previousQuantity}</td>
                    <td>${item.newQuantity}</td>
                    <td>${escapeHtml(item.performedBy || "-")}</td>
                    <td>${escapeHtml(referenceText)}</td>
                </tr>
            `;
        }).join("");
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}

async function updateTransferStatus(id, action) {
    const actionLabel = action === "APPROVE" ? "approve and dispatch stock for" :
                        action === "COMPLETE" ? "mark stock receipt as completed for" : "reject/cancel";

    if (!confirm(`Are you sure you want to ${actionLabel} transfer request #${id}?`)) return;

    try {
        const res = await authFetch(`${API_BASE}/operations/transfers/${id}/status`, {
            method: "PUT",
            body: JSON.stringify({ action })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to update transfer status");

        showAlert("ops-alert", `Transfer request #${id} updated to ${data.status}!`);
        await loadTransfers();
        await loadStocks();
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}

async function loadPurchases() {
    const filterEl = document.getElementById("purchase-branch-filter");
    const branchId = filterEl && filterEl.value ? filterEl.value : "";
    const url = branchId ? `${API_BASE}/operations/purchases?branchId=${branchId}` : `${API_BASE}/operations/purchases`;

    try {
        const res = await authFetch(url);
        if (!res.ok) throw new Error("Failed to load purchase requests");
        const list = await res.json();

        const tbody = document.getElementById("purchases-table-body");
        if (tbody) {
            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: #64748b; padding: 1.5rem;">No purchase requests found.</td></tr>`;
            } else {
                tbody.innerHTML = list.map(item => {
                    let badgeClass = "badge-pending";
                    if (item.status === "APPROVED") badgeClass = "badge-approved";
                    else if (item.status === "REJECTED") badgeClass = "badge-rejected";

                    let actionButtons = "";
                    if (getUser()?.role !== "ADMIN") {
                        actionButtons = `<span style="color: #64748b; font-size: 0.8rem;">Admin approval required</span>`;
                    } else if (item.status === "PENDING") {
                        actionButtons = `
                            <button class="btn btn-sm btn-success" onclick="updatePurchaseStatus(${item.id}, 'APPROVE')">Approve</button>
                            <button class="btn btn-sm btn-danger" onclick="updatePurchaseStatus(${item.id}, 'REJECT')">Reject</button>
                        `;
                    } else {
                        actionButtons = `<span style="color: #64748b; font-size: 0.8rem;">Finished</span>`;
                    }

                    return `
                        <tr>
                            <td>#${item.id}</td>
                            <td>${escapeHtml(item.branchName)}</td>
                            <td>${escapeHtml(item.productName)} (<strong>${escapeHtml(item.productSku)}</strong>)</td>
                            <td style="font-weight: bold;">${item.requestedQuantity}</td>
                            <td><span class="badge ${badgeClass}">${item.status}</span></td>
                            <td>${item.createdAt ? item.createdAt.substring(0, 10) : "-"}</td>
                            <td>${actionButtons}</td>
                        </tr>
                    `;
                }).join("");
            }
        }
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}

async function updatePurchaseStatus(id, action) {
    if (!confirm(`Are you sure you want to ${action.toLowerCase()} purchase request #${id}?`)) return;

    try {
        const res = await authFetch(`${API_BASE}/operations/purchases/${id}/status`, {
            method: "PUT",
            body: JSON.stringify({ action })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to update purchase request status");

        showAlert("ops-alert", `Purchase request #${id} updated to ${data.status}!`);
        await loadPurchases();
    } catch (err) {
        showAlert("ops-alert", err.message, true);
    }
}
