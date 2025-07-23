
const API_BASE = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', function() {
    
    
    const productModalElement = document.getElementById('productModal');
    const productModal = productModalElement ? new bootstrap.Modal(productModalElement) : null;

    const cartModalElement = document.getElementById('cartModal');
    const cartModal = cartModalElement ? new bootstrap.Modal(cartModalElement) : null;

    const orderConfirmationModalElement = document.getElementById('orderConfirmationModal');
    const orderConfirmationModal = orderConfirmationModalElement ? new bootstrap.Modal(orderConfirmationModalElement) : null;

    const orderCompleteModalElement = document.getElementById('orderCompleteModal');
    const orderCompleteModal = orderCompleteModalElement ? new bootstrap.Modal(orderCompleteModalElement) : null;

    
    let currentOrderData = {
        customerInfo: {
            customerId: null,
            name: '',
            email: '',
            address: '',
            phoneNumber: ''
        },
        paymentMethod: '',
        items: [],
        totalPrice: 0
    };

    
    let currentSelectedCategory = 'all';
    let currentSearchTerm = '';
    let allProducts = [];

    
    async function handleError(response, defaultMessage) {
        let errorMessage = defaultMessage;
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || defaultMessage;
        } catch (e) {
            
        }
        console.error('Error:', errorMessage);
        alert(errorMessage);
        throw new Error(errorMessage);
    }

    
    function toggleModal(modalInstance, show) {
        
        if (modalInstance) {
            if (show) {
                modalInstance.show();
            } else {
                modalInstance.hide();
            }
        } else {
            
            
        }
    }

    /**
     * ヘッダーの右側ボタン部分をログイン状態に応じて更新する
     * @param {boolean} loggedIn
     * @param {string} [userName='']
     */
    async function updateHeaderButtons(loggedIn, userName = '') {
        const headerRightButtons = document.getElementById("header-right-buttons");
        if (!headerRightButtons) {
            
            
            console.warn("Header right buttons container not found!"); 
            return;
        }

        let buttonsHtml = '';
        if (loggedIn) {
            
            buttonsHtml = `
                <span class="navbar-text me-2">${userName}さん</span>
                <button id="cart-btn" class="btn btn-outline-dark position-relative me-2">
                    <i class="bi bi-cart"></i> カート
                    <span id="cart-count" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger cart-badge">
                        0
                    </span>
                </button>
                <button class="btn btn-outline-dark" id="logoutBtn">ログアウト</button>
            `;
        } else {
            
            buttonsHtml = `
                <button id="cart-btn" class="btn btn-outline-dark position-relative me-2">
                    <i class="bi bi-cart"></i> カート
                    <span id="cart-count" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger cart-badge">
                        0
                    </span>
                </button>
                <button class="btn btn-outline-dark">
                    <a href="C0601.html" class="text-light text-decoration-none">ログイン / 新規会員登録</a>
                </button>
            `;
        }
        headerRightButtons.innerHTML = buttonsHtml;

        const cartBtn = document.getElementById("cart-btn");
        if (cartBtn && cartModal) { 
            cartBtn.addEventListener("click", showCartModal);
        }

        if (loggedIn) {
            const logoutBtn = document.getElementById("logoutBtn");
            if (logoutBtn) {
                logoutBtn.addEventListener("click", async function(){
                    try {
                        const logoutResponse = await fetch('/api/customers/logout', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' }
                        });

                        if (logoutResponse.ok) {
                            sessionStorage.removeItem("userName");
                            currentOrderData.customerId = null;
                            window.location.reload();
                        } else {
                            const errorData = await logoutResponse.json();
                            console.error("ログアウト失敗:", errorData.message);
                            alert("ログアウトに失敗しました: " + (errorData.message || "不明なエラー"));
                        }
                    } catch (error) {
                        console.error("ログアウトエラー:", error);
                        alert("ネットワークエラーによりログアウトできませんでした。");
                    }
                });
            }
        }
        
        const cartCountElement = document.getElementById('cart-count');
        if (cartCountElement) {
            updateCartDisplay();
        }
    }

    async function initializeHeader() {
        try {
            const response = await fetch('/api/customers/status');
            const data = await response.json();

            if (response.ok && data.loggedIn) {
                updateHeaderButtons(true, data.customerName);
                currentOrderData.customerInfo.customerId = data.customerId || null; 
                console.log("Logged in customer ID set:", currentOrderData.customerInfo.customerId);
            } else {
                updateHeaderButtons(false);
                currentOrderData.customerInfo.customerId = null;
            }
        } catch (error) {
            console.error('ログイン状態確認エラー:', error);
            updateHeaderButtons(false);
            currentOrderData.customerInfo.customerId = null;
        }
    }

    initializeHeader();

    
    const productsContainer = document.getElementById('products-container');
    const searchInput = document.getElementById('searchInput');
    const categoryButtons = document.querySelectorAll('.category-btn'); 

    if (productsContainer && searchInput && categoryButtons.length > 0) {
        fetchAndDisplayProducts();

        searchInput.addEventListener('input', function() {
            currentSearchTerm = this.value.toLowerCase();
            displayFilteredProducts(); 
        });

        categoryButtons.forEach(button => {
            button.addEventListener('click', function() {
                currentSelectedCategory = this.dataset.category;
                categoryButtons.forEach(btn => btn.classList.remove('active')); 
                this.classList.add('active');
                displayFilteredProducts();
            });
        });
    }


    async function fetchAndDisplayProducts() {
        
        
        const container = document.getElementById('products-container');
        if (!container) {
            console.error("Product container not found! (This should not happen if called correctly)");
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/products`);
            if (!response.ok) {
                await handleError(response, '商品の取得に失敗しました');
            }
            allProducts = await response.json();
            displayFilteredProducts();
        } catch (error) {
            console.error(error.message);
        }
    }

    function displayFilteredProducts() {
        
        
        const container = document.getElementById('products-container');
        if (!container) {
            console.error("Product container not found! (This should not happen if called correctly)");
            return;
        }


    document.querySelectorAll('.category-btn').forEach(button => {
        button.addEventListener('click', function() {
            console.log("カテゴリボタンがクリックされました:", this.dataset.category);
            fetchAndDisplayProducts(this.dataset.category);  // カテゴリ別取得
        });
    });
});



    async function fetchProductDetail(productId) {
        try {
            const response = await fetch(`${API_BASE}/products/${productId}`);
            if (!response.ok) {
                await handleError(response, '商品詳細の取得に失敗しました');
            }
            const product = await response.json();
            displayProductDetail(product);
        } catch (error) {
            console.error(error.message);
        }
    }

    function displayProductDetail(product) {
        
        const productModalTitle = document.getElementById('productModalTitle');
        const productModalBody = document.getElementById('productModalBody');
        if (!productModalTitle || !productModalBody) {
            console.warn("Product detail modal elements not found.");
            return; 
        }

        productModalTitle.textContent = product.name;
        productModalBody.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <img src="${product.imageUrl || 'https://via.placeholder.com/400x300'}" class="img-fluid" alt="${product.name}">
                </div>
                <div class="col-md-6">
                    <p class="fs-4">¥${product.price.toLocaleString()}</p>
                    <p>${product.description}</p>
                    <p>在庫: <span id="product-stock">${product.stock}</span> 個</p>
                    <div class="d-flex align-items-center mb-3">
                        <label for="quantity">数量:</label>
                        <input type="number" id="quantity" class="form-control w-25" value="1" min="1" max="${product.stock}">
                    </div>
                    <button class="btn btn-primary add-to-cart" data-id="${product.productId}">カートに入れる</button>
                </div>
            </div>
        `;

        const addToCartButton = productModalBody.querySelector('.add-to-cart');
        if (addToCartButton) { 
            addToCartButton.addEventListener('click', function() {
                const quantityInput = document.getElementById('quantity');
                const quantity = parseInt(quantityInput.value);
                const stock = parseInt(document.getElementById('product-stock').textContent);

                
                if (quantity <= 0 || isNaN(quantity)) {
                    alert('数量は1以上で入力してください。');
                    quantityInput.value = 1;
                    return;
                }
                if (quantity > stock) {
                    alert(`数量は在庫数(${stock})以下で入力してください。`);
                    quantityInput.value = stock;
                    return;
                }
                addToCart(product.productId, quantity);
            });
        }

        toggleModal(productModal, true);
    }

    async function addToCart(productId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, quantity })
            });

            if (!response.ok) {
                await handleError(response, 'カートへの追加に失敗しました');
            }

            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
            toggleModal(productModal, false);
            alert('商品をカートに追加しました');
        } catch (error) {
            console.error(error.message);
        }
    }

    async function fetchLoggedInCustomerInfo() {
        try {
            const response = await fetch(`${API_BASE}/customers/profile`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (response.ok) {
                const customer = await response.json();
                console.log("Fetched logged-in customer data:", customer);
                return customer;
            } else if (response.status === 401) {
                return null;
            } else {
                
                const errorData = await response.json().catch(() => ({ message: '不明なエラー' }));
                console.error(`Failed to fetch customer info: ${response.status} - ${errorData.message}`);
                return null;
            }
        } catch (error) {
            console.error('Error fetching logged-in customer info:', error);
            return null;
        }
    }

    async function updateCartDisplay() {
        try {
            const response = await fetch(`${API_BASE}/cart`);
            if (!response.ok) {
                await handleError(response, 'カート情報の取得に失敗しました');
            }
            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error(error.message);
        }
    }

    function updateCartBadge(count) {
        const cartCountElement = document.getElementById('cart-count');
        if (cartCountElement) {
            cartCountElement.textContent = count;
        } else {
            console.warn("Cart count element not found!");
        }
    }

    
    
    
    async function showCartModal() {
        if (cartModal) { 
            await updateCartModalContent();
            toggleModal(cartModal, true);
        } else {
            console.warn("Attempted to show cart modal, but cartModal instance is not available.");
        }
    }

    async function updateCartModalContent(showCheckoutForm = false) {
        const modalTitle = document.getElementById('cartModalTitle');
        const modalBody = document.getElementById('cartModalBody');
        const modalFooter = document.getElementById('cartModalFooter');

        
        if (!modalTitle || !modalBody || !modalFooter) {
            console.warn("Cart modal content elements not found.");
            return;
        }

        if (!showCheckoutForm) {
            modalTitle.textContent = 'ショッピングカート';
            try {
                const response = await fetch(`${API_BASE}/cart`);
                if (!response.ok) {
                    await handleError(response, 'カート情報の取得に失敗しました');
                }
                const cart = await response.json();


                if (cart.items && Object.keys(cart.items).length > 0) {
                    const { shippingFee, grandTotal } = cart;

                    let html = `
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>商品</th>
                                    <th>単価</th>
                                    <th>数量</th>
                                    <th>小計</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                    `;

                    Object.values(cart.items).forEach(item => {
                        html += `
                                <tr>
                                    <td>${item.name}</td>
                                    <td>¥${item.price.toLocaleString()}</td>
                                    <td>
                                        <input type="number" class="form-control form-control-sm update-quantity"
                                                 data-id="${item.id}" value="${item.quantity}" min="1" max="${item.stock}" style="width: 70px">
                                    </td>
                                    <td>¥${item.subtotal.toLocaleString()}</td>
                                    <td>
                                        <button class="btn btn-sm btn-danger remove-item" data-id="${item.id}">削除</button>
                                    </td>
                                </tr>
                        `;
                    });

                    html += `
                            </tbody>
                            <tfoot>
                                <tr>
                                    <th colspan="3" class="text-end">商品合計:</th>
                                    <th>¥${cart.totalPrice.toLocaleString()}</th>
                                    <th></th>
                                </tr>
                                <tr>
                                    <th colspan="3" class="text-end">送料:</th>
                                    <th>¥${shippingFee.toLocaleString()}</th>
                                    <th></th>
                                </tr>
                                <tr>
                                    <th colspan="3" class="text-end fs-5">最終合計:</th>
                                    <th class="fs-5">¥${grandTotal.toLocaleString()}</th>
                                    <th></th>
                                </tr>
                            </tfoot>
                        </table>
                    `;

                    modalBody.innerHTML = html;

                    
                    document.querySelectorAll('.update-quantity').forEach(input => {
                        input.addEventListener('change', function() {
                            const newQuantity = parseInt(this.value);
                            const itemId = this.dataset.id;
                            const maxStock = parseInt(this.max);

                            if (newQuantity <= 0 || isNaN(newQuantity)) {
                                alert('数量は1以上で入力してください。');
                                this.value = 1;
                                return;
                            }
                            if (newQuantity > maxStock) {
                                alert(`数量は在庫数(${maxStock})以下で入力してください。`);
                                this.value = maxStock;
                                return;
                            }
                            updateItemQuantity(itemId, newQuantity);
                        });
                    });

                    document.querySelectorAll('.remove-item').forEach(button => {
                        button.addEventListener('click', function() {
                            removeItem(this.dataset.id);
                        });
                    });

                    modalFooter.innerHTML = `
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">買い物を続ける</button>
                        <button type="button" class="btn btn-secondary" id="login-to-purchase-btn">ログインして購入</button>
                        <button type="button" class="btn btn-primary" id="proceed-to-checkout-form">注文手続きへ</button>
                    `;
                    const proceedToCheckoutFormBtn = document.getElementById('proceed-to-checkout-form');
                    if (proceedToCheckoutFormBtn) { 
                        proceedToCheckoutFormBtn.addEventListener('click', () => updateCartModalContent(true));
                    }

                    const loginToPurchaseBtn = document.getElementById('login-to-purchase-btn');
                    if (loginToPurchaseBtn) { 
                        loginToPurchaseBtn.addEventListener('click', async () => {
                            try {
                                const statusResponse = await fetch('/api/customers/status');
                                const statusData = await statusResponse.json();
                                if (statusResponse.ok && statusData.loggedIn) {
                                    alert('すでにログインしています。注文手続きへ進みます。');
                                    updateCartModalContent(true); 
                                } else {
                                    window.location.href = 'C0601.html'; 
                                }
                            } catch (error) {
                                console.error('ログイン状態確認エラー:', error);
                                alert('エラーが発生しました。ログインページへ遷移します。');
                                window.location.href = 'C0601.html';
                            }
                        });
                    }

                } else {
                    modalBody.innerHTML = '<p class="text-center">カートは空です</p>';
                    modalFooter.innerHTML = `<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">閉じる</button>`;
                }
            } catch (error) {
                console.error(error.message);
                modalBody.innerHTML = '<p class="text-center text-danger">カート情報の読み込みに失敗しました。</p>';
                modalFooter.innerHTML = `<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">閉じる</button>`;
            }
        } else {
            
            modalTitle.textContent = 'お客様情報入力';
            modalBody.innerHTML = `
                <form id="order-form" class="needs-validation" novalidate>
                    <div class="mb-3">
                        <label for="name" class="form-label">お名前</label>
                        <input type="text" class="form-control" id="name" required pattern=".{2,}">
                        <div class="invalid-feedback">お名前は2文字以上で入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">メールアドレス</label>
                        <input type="email" class="form-control" id="email" required>
                        <div class="invalid-feedback">有効なメールアドレスを入力してください (例: user@example.com)</div>
                    </div>
                    <div class="mb-3">
                        <label for="address" class="form-label">住所</label>
                        <input type="text" class="form-control" id="address" required pattern=".{5,}">
                        <div class="invalid-feedback">住所は5文字以上で入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="phone" class="form-label">電話番号</label>
                        <input type="tel" class="form-control" id="phone" required pattern="^0\\d{9,10}$">
                        <div class="invalid-feedback">有効な電話番号を入力してください (ハイフンなし、0から始まる10桁または11桁)</div>
                    </div>

                    <hr class="my-4">

                    <h5>決済方法の選択</h5>
                    <div class="mb-3" id="paymentMethodRadios">
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentBankTransfer" value="bank_transfer" required>
                            <label class="form-check-label" for="paymentBankTransfer">
                                銀行振込
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentCashOnDelivery" value="cash_on_delivery" required>
                            <label class="form-check-label" for="paymentCashOnDelivery">
                                代金引換
                            </label>
                        </div>
                        <div class="invalid-feedback" id="paymentMethodFeedback">決済方法を選択してください</div>
                    </div>
                </form>
            `;
            modalFooter.innerHTML = `
                <button type="button" class="btn btn-secondary" id="back-to-cart">カートに戻る</button>
                <button type="button" class="btn btn-primary" id="submit-order-form-and-show-confirmation">注文内容を確認する</button>
            `;
            
            const backToCartBtn = document.getElementById('back-to-cart');
            if (backToCartBtn) {
                backToCartBtn.addEventListener('click', () => updateCartModalContent(false));
            }
            const submitOrderFormAndShowConfirmationBtn = document.getElementById('submit-order-form-and-show-confirmation');
            if (submitOrderFormAndShowConfirmationBtn) {
                submitOrderFormAndShowConfirmationBtn.addEventListener('click', submitOrderFormAndShowConfirmation);
            }
            
            const customer = await fetchLoggedInCustomerInfo();
            if (customer) {
                const nameInput = document.getElementById('name');
                const emailInput = document.getElementById('email');
                const addressInput = document.getElementById('address');
                const phoneInput = document.getElementById('phone');

                if (nameInput) nameInput.value = customer.name || '';
                if (emailInput) emailInput.value = customer.email || '';
                if (addressInput) addressInput.value = customer.address || '';
                if (phoneInput) phoneInput.value = customer.phoneNumber || '';
            } else {
                const nameInput = document.getElementById('name');
                const emailInput = document.getElementById('email');
                const addressInput = document.getElementById('address');
                const phoneInput = document.getElementById('phone');

                if (nameInput) nameInput.value = currentOrderData.customerInfo.name || '';
                if (emailInput) emailInput.value = currentOrderData.customerInfo.email || '';
                if (addressInput) addressInput.value = currentOrderData.customerInfo.address || '';
                if (phoneInput) phoneInput.value = currentOrderData.customerInfo.phoneNumber || '';
            }
            if (currentOrderData.paymentMethod) {
                const radio = document.querySelector(`input[name="paymentMethod"][value="${currentOrderData.paymentMethod}"]`);
                if (radio) radio.checked = true;
            }
            const form = document.getElementById('order-form');
            if (form) { 
                form.querySelectorAll('input, select').forEach(input => {
                    const validateInput = () => {
                        if (input.checkValidity()) {
                            input.classList.remove('is-invalid');
                            input.classList.add('is-valid');
                        } else {
                            input.classList.remove('is-valid');
                            input.classList.add('is-invalid');
                        }
                    };
                    input.addEventListener('input', validateInput);
                    input.addEventListener('blur', validateInput);
                });
            }
            const paymentRadios = document.querySelectorAll('input[name="paymentMethod"]');
            const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
            if (paymentRadios.length > 0 && paymentMethodFeedback) { 
                paymentRadios.forEach(radio => {
                    radio.addEventListener('change', () => {
                        if (document.querySelector('input[name="paymentMethod"]:checked')) {
                            paymentMethodFeedback.style.display = 'none';
                        } else {
                            paymentMethodFeedback.style.display = 'block';
                        }
                    });
                });
                
                if (document.querySelector('input[name="paymentMethod"]:checked')) {
                    paymentMethodFeedback.style.display = 'none';
                } else {
                    paymentMethodFeedback.style.display = 'block';
                }
            }
        }
    }
    async function updateItemQuantity(itemId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity: parseInt(quantity, 10) }) 
            });

            if (!response.ok) {
                await handleError(response, '数量の更新に失敗しました。');
                return; 
            }

            const cart = await response.json();
            updateCartModalContent();
            updateCartBadge(cart.totalQuantity);
            
            
        } catch (error) {
            console.error('商品数量更新エラー:', error);
            
            
            updateCartModalContent(); 
        }
    }

    async function removeItem(itemId) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                await handleError(response, '商品の削除に失敗しました。');
                return; 
            }

            const cart = await response.json();
            updateCartModalContent();
            updateCartBadge(cart.totalQuantity);
            
            
        } catch (error) {
            console.error('商品削除エラー:', error);
            
        }
    }

    async function submitOrderFormAndShowConfirmation() {
        const form = document.getElementById('order-form');
        if (!form) {
            console.error('注文フォームが見つかりません。');
            alert('システムエラー: 注文フォームが見つかりません。');
            return;
        }

        
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            form.querySelector(':invalid')?.focus();
            return;
        }

        const paymentMethodElement = document.querySelector('input[name="paymentMethod"]:checked');
        const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');

        if (paymentMethodFeedback) { 
            if (!paymentMethodElement) {
                paymentMethodFeedback.style.display = 'block';
                alert('決済方法を選択してください。');
                return;
            } else {
                paymentMethodFeedback.style.display = 'none';
            }
        } else {
            
            console.warn('paymentMethodFeedback 要素が見つかりません。');
        }


        
        const nameInput = document.getElementById('name');
        const emailInput = document.getElementById('email');
        const addressInput = document.getElementById('address');
        const phoneInput = document.getElementById('phone');
        const existingCustomerId = currentOrderData.customerInfo.customerId;
        currentOrderData.customerInfo = {
            customerId: existingCustomerId ,
            name: nameInput ? nameInput.value : '',
            email: emailInput ? emailInput.value : '',
            address: addressInput ? addressInput.value : '',
            phoneNumber: phoneInput ? phoneInput.value : ''
        };

        currentOrderData.paymentMethod = paymentMethodElement ? paymentMethodElement.value : '';

        try {
            const cartResponse = await fetch(`${API_BASE}/cart`);
            if (!cartResponse.ok) {
                await handleError(cartResponse, 'カート情報の取得に失敗しました。');
                return; 
            }
            const cart = await cartResponse.json();

            
            if (!cart.items || Object.values(cart.items).length === 0) {
                alert('カートに商品がありません。商品を追加してから注文してください。');
                return;
            }

            currentOrderData.items = Object.values(cart.items);
            currentOrderData.totalPrice = cart.totalPrice;
            currentOrderData.shippingFee = cart.shippingFee;
            currentOrderData.grandTotal = cart.totalPrice + cart.shippingFee; 

            if (cartModal) toggleModal(cartModal, false); 
            showOrderConfirmation();
        } catch (error) {
            console.error('注文確認準備エラー:', error);
            alert(`注文情報の準備中にエラーが発生しました。もう一度お試しください: ${error.message}`);
        }
    }

    function showOrderConfirmation() {
        const orderConfirmationModalTitle = document.getElementById('orderConfirmationModalTitle');
        const modalBody = document.getElementById('orderConfirmationModalBody');
        const modalFooter = document.getElementById('orderConfirmationModalFooter');

        if (!orderConfirmationModalTitle || !modalBody || !modalFooter) {
            console.error('注文確認モーダルの要素が見つかりません。');
            alert('システムエラー: 注文確認画面を表示できません。');
            return;
        }

        orderConfirmationModalTitle.textContent = '注文内容の確認';

        let itemsHtml = `
            <div class="confirmation-box">
                <div class="section-title">注文商品</div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>商品名</th>
                            <th>数量</th>
                            <th>単価</th>
                            <th>小計</th>
                        </tr>
                    </thead>
                    <tbody>
        `;
        if (currentOrderData.items && currentOrderData.items.length > 0) {
            currentOrderData.items.forEach(item => {
                itemsHtml += `
                    <tr>
                        <td>${item.name}</td>
                        <td>${item.quantity}</td>
                        <td>¥${item.price.toLocaleString()}</td>
                        <td>¥${(item.quantity * item.price).toLocaleString()}</td>
                    </tr>
                `;
            });
        } else {
            itemsHtml += `<tr><td colspan="4" class="text-center">カートに商品がありません。</td></tr>`;
        }

        itemsHtml += `
                    </tbody>
                    <tfoot>
                        <tr>
                            <th colspan="3" class="text-end">商品合計:</th>
                            <th>¥${(currentOrderData.totalPrice || 0).toLocaleString()}</th>
                        </tr>
                        <tr>
                            <th colspan="3" class="text-end">送料:</th>
                            <th>¥${(currentOrderData.shippingFee || 0).toLocaleString()}</th>
                        </tr>
                        <tr>
                            <th colspan="3" class="text-end fs-5">最終合計:</th>
                            <th class="fs-5">¥${(currentOrderData.totalPrice + currentOrderData.shippingFee || 0).toLocaleString()}</th>
                        </tr>
                    </tfoot>
                </table>
            </div>
        `;

        const customerInfo = currentOrderData.customerInfo || {};
        let customerHtml = `
            <div class="confirmation-box">
                <div class="section-title">お届け先</div>
                <dl class="row">
                    <dt class="col-sm-4">お名前</dt>
                    <dd class="col-sm-8">${customerInfo.name || '未入力'}</dd>

                    <dt class="col-sm-4">メールアドレス</dt>
                    <dd class="col-sm-8">${customerInfo.email || '未入力'}</dd>

                    <dt class="col-sm-4">住所</dt>
                    <dd class="col-sm-8">${customerInfo.address || '未入力'}</dd>

                    <dt class="col-sm-4">電話番号</dt>
                    <dd class="col-sm-8">${customerInfo.phoneNumber || '未入力'}</dd>
                </dl>
            </div>
        `;

        const displayPaymentMethod = (() => {
            switch (currentOrderData.paymentMethod) {
                case 'bank_transfer': return '銀行振込';
                case 'cash_on_delivery': return '代金引換';
                case 'credit_card': return 'クレジットカード'; 
                default: return '未選択';
            }
        })();

        let paymentHtml = `
            <div class="confirmation-box">
                <div class="section-title">お支払い方法</div>
                <p>${displayPaymentMethod}</p>
            </div>
        `;

        modalBody.innerHTML = itemsHtml + customerHtml + paymentHtml;

        modalFooter.innerHTML = `
            <button type="button" class="btn btn-secondary" id="back-to-customer-form">戻る</button>
            <button type="button" class="btn btn-primary" id="final-confirm-order-btn">注文を確定する</button>
        `;

        
        const backButton = document.getElementById('back-to-customer-form');
        const confirmButton = document.getElementById('final-confirm-order-btn');

        if (backButton) {
            backButton.onclick = null; 
            backButton.addEventListener('click', function() {
                if (orderConfirmationModal) toggleModal(orderConfirmationModal, false);
                if (cartModal) toggleModal(cartModal, true);
                updateCartModalContent(true);
            });
        }

        if (confirmButton) {
            confirmButton.onclick = null; 
            confirmButton.addEventListener('click', confirmOrder);
        }

        if (orderConfirmationModal) toggleModal(orderConfirmationModal, true);
    }

    async function confirmOrder() {
        try {
            console.log("Sending currentOrderData to backend:", JSON.stringify(currentOrderData, null, 2));
            
            const response = await fetch(`${API_BASE}/order/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(currentOrderData)
            });

            if (!response.ok) {
                await handleError(response, '注文の確定に失敗しました。');
                return;
            }

            const orderResult = await response.json();

            
            try {
            // カートをクリアするAPIリクエスト
            const cartClearResponse = await fetch(`${API_BASE}/cart`, {
                method: 'DELETE', // DELETE メソッドを使用
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include' // セッションクッキーを送信
            });

            if (!cartClearResponse.ok) {
                // カートのクリアは注文本体とは独立してエラーハンドリング
                const errorData = await cartClearResponse.json().catch(() => ({ message: '不明なエラー' }));
                console.error(`Failed to clear cart: ${cartClearResponse.status} - ${errorData.message}`);
                // ユーザーには注文が完了したことを伝えつつ、カートクリアの問題を軽く伝える
                alert('注文は正常に完了しましたが、カートのクリア中に問題が発生しました。');
            } else {
                console.log('カートをクリアしました。');
                updateCartBadge(0); // カートバッジを0に更新
            }
        } catch (cartClearError) {
            // カートクリア処理でのネットワークエラーなど
            console.error('カートクリア中のネットワークエラー:', cartClearError);
            alert('注文は正常に完了しましたが、カートのクリア中にネットワークエラーが発生しました。');
        }
            
            const orderForm = document.getElementById('order-form');
            if (orderForm) {
                orderForm.reset();
                orderForm.classList.remove('was-validated');
                orderForm.querySelectorAll('.is-valid, .is-invalid').forEach(el => {
                    el.classList.remove('is-valid', 'is-invalid');
                });
                const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
                if (paymentMethodFeedback) {
                    paymentMethodFeedback.style.display = 'block'; 
                }
            }

            
            currentOrderData = {
                customerInfo: {
                    customerId: currentOrderData.customerId || null, 
                    name: '', email: '', address: '', phoneNumber: ''
                },
                paymentMethod: '',
                items: [],
                totalPrice: 0,
                shippingFee: 0,
                grandTotal: 0
            };

            if (orderConfirmationModal) toggleModal(orderConfirmationModal, false);
            displayOrderComplete(orderResult);
            if (orderCompleteModal) toggleModal(orderCompleteModal, true);
            
            

        } catch (error) {
            console.error('注文確定エラー:', error);
            alert(`注文確定中にエラーが発生しました。もう一度お試しください: ${error.message}`);
        }
    }

    function displayOrderComplete(order) {
        const orderCompleteModalTitle = document.getElementById('orderCompleteModalTitle');
        const modalBody = document.getElementById('orderCompleteModalBody');
        const modalFooter = document.getElementById('orderCompleteModalFooter');

        if (!orderCompleteModalTitle || !modalBody || !modalFooter) {
            console.error('注文完了モーダルの要素が見つかりません。');
            alert('システムエラー: 注文完了画面を表示できません。');
            return;
        }

        orderCompleteModalTitle.textContent = 'ご注文完了';

        const displayPaymentMethod = (() => {
            switch (order.paymentMethod) {
                case 'bank_transfer': return '銀行振込';
                case 'cash_on_delivery': return '代金引換';
                case 'credit_card': return 'クレジットカード';
                default: return order.paymentMethod || '';
            }
        })();

        modalBody.innerHTML = `
            <p>ご注文ありがとうございます。注文番号は <strong>${order.orderId || 'N/A'}</strong> です。</p>
            <p>ご注文日時: ${order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A'}</p>
            <p>決済方法: ${displayPaymentMethod}</p>
            <p>商品合計: ¥${(order.totalPrice || 0).toLocaleString()}</p>
            <p>送料: ¥${(order.shippingFee || 0).toLocaleString()}</p>
            <p class="fs-5">最終お支払い金額: ¥${(order.grandTotal || 0).toLocaleString()}</p>
        `;
        modalFooter.innerHTML = `<button type="button" class="btn btn-primary" data-bs-dismiss="modal">閉じる</button>`;
    }

    
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('showCart') === 'true' && urlParams.get('showCheckoutForm') === 'true') {
        
        
        if (cartModal) { 
            cartModal.show();
            updateCartModalContent(true);
            history.replaceState({}, document.title, window.location.pathname);
        } else {
            console.warn('cartModal インスタンスが初期化されていません。');
        }
    }
    if (window.location.pathname.includes('C0601.html')) {
        initializeAuthPageFeatures();
    }
});

function initializeAuthPageFeatures() {
    const showRegisterBtn = document.getElementById("show-register-btn");
    const loginContainer = document.getElementById("login-container");
    const registerContainer = document.getElementById("register-container");
    if (showRegisterBtn && loginContainer && registerContainer) {
        showRegisterBtn.addEventListener("click", function() {
            loginContainer.style.display = "none";
            registerContainer.style.display = "block";
        });
    } else {
        console.warn('認証関連のDOM要素が一部見つかりません。');
    }

    const registerForm = document.getElementById("registerForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async function(e) {
            e.preventDefault();

            const name = e.target.name?.value || ''; 
            const email = e.target.email?.value || '';
            const address = e.target.address?.value || '';
            const phoneNumber = e.target.phoneNumber?.value || '';
            const password = e.target.password?.value || '';

            const requestBody = {
                customerInfo: { name, email, address, phoneNumber },
                password: password
            };

            const registerMessageElement = document.getElementById("registerMessage");

            try {
                const response = await fetch(`${API_BASE}/customers/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestBody)
                });

                const data = await response.json();

                if (response.ok) {
                    sessionStorage.setItem("userName", data.name + "さん");
                    if (registerMessageElement) {
                        registerMessageElement.textContent = "会員登録が完了しました！";
                        registerMessageElement.style.color = "#388e3c"; 
                    }
                    setTimeout(() => {
                        window.location.href = "index.html";
                    }, 2000);
                } else {
                    if (registerMessageElement) {
                        registerMessageElement.textContent = "登録失敗: " + (data.message || "不明なエラーが発生しました。");
                        registerMessageElement.style.color = "red";
                    }
                }
            } catch (error) {
                console.error('会員登録エラー:', error);
                if (registerMessageElement) {
                    registerMessageElement.textContent = "ネットワークエラーが発生しました。インターネット接続を確認してください。";
                    registerMessageElement.style.color = "red";
                }
            }
        });
    }

    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        loginForm.addEventListener("submit", async function(e) {
            e.preventDefault();

            
            const emailInput = loginForm.querySelector("#email");
            const passwordInput = loginForm.querySelector("#password");

            const email = emailInput ? emailInput.value : '';
            const password = passwordInput ? passwordInput.value : '';

            const requestBody = { email, password };
            let loginErrorElement = loginForm.querySelector('.message'); 

            try {
                const response = await fetch(`${API_BASE}/customers/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(requestBody)
                });

                const data = await response.json();

                if (response.ok) {
                    sessionStorage.setItem("userName", data.name + "さん");
                    window.location.href = "index.html";
                } else {
                    if (!loginErrorElement) { 
                        loginErrorElement = document.createElement('div');
                        loginErrorElement.className = 'message';
                        loginErrorElement.style.color = 'red';
                        loginForm.prepend(loginErrorElement); 
                    }
                    loginErrorElement.textContent = data.message || "ログイン失敗: メールアドレスまたはパスワードが正しくありません。";
                }
            } catch (error) {
                console.error('ログインエラー:', error);
                if (!loginErrorElement) { 
                    loginErrorElement = document.createElement('div');
                    loginErrorElement.className = 'message';
                    loginErrorElement.style.color = 'red';
                    loginForm.prepend(loginErrorElement); 
                }
                loginErrorElement.textContent = "ネットワークエラーが発生しました。インターネット接続を確認してください。";
            }
        });
    }

    const registerPasswordField = document.getElementById("registerPassword");
    if (registerPasswordField) {
        const toggleButton = registerPasswordField.nextElementSibling;
        if (toggleButton && toggleButton.classList.contains('password-toggle-btn')) {
            toggleButton.addEventListener('click', () => togglePasswordVisibility('registerPassword'));
        }
    }

    const loginPasswordField = document.getElementById("password"); 
    if (loginPasswordField) {
        const toggleButton = loginPasswordField.nextElementSibling;
        if (toggleButton && toggleButton.classList.contains('password-toggle-btn')) {
            toggleButton.addEventListener('click', () => togglePasswordVisibility('password'));
        }
    }
}









window.addEventListener("load", async function(){
    try {
        const response = await fetch('/api/customers/status');
        const data = await response.json();

        if (response.ok && data.loggedIn) {
            
            if (!window.location.pathname.includes("C0601.html") && !window.location.pathname.includes("index.html")) {
                sessionStorage.setItem("userName", data.customerName + "さん");
                window.location.href = "index.html";
            } else if (window.location.pathname.includes("C0601.html")) {
                
                alert("すでにログインしています。トップページへ移動します。");
                window.location.href = "index.html";
            }
        }
    } catch (error) {
        console.error('ログイン状態確認エラー:', error);
        
    }
});



function togglePasswordVisibility(id) {
    const passwordField = document.getElementById(id);
    if (!passwordField) {
        console.warn(`パスワードフィールドID "${id}" が見つかりません。`);
        return;
    }
    const toggleButton = passwordField.nextElementSibling;


    if (toggleButton && toggleButton.classList.contains('password-toggle-btn')) {
        if (passwordField.type === 'password') {
            passwordField.type = 'text';
            toggleButton.textContent = '隠す';
        } else {
            passwordField.type = 'password';
            toggleButton.textContent = '表示';
        }

    } else {
        console.warn(`パスワードフィールド "${id}" の切り替えボタンが見つからないか、クラスが正しくありません。`);
    }

}
