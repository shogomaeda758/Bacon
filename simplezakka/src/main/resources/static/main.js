const API_BASE = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', function() {
    // --- モーダルインスタンスの取得 ---
    const productModalElement = document.getElementById('productModal');
    const productModal = productModalElement ? new bootstrap.Modal(productModalElement) : null;

    const cartModalElement = document.getElementById('cartModal');
    const cartModal = cartModalElement ? new bootstrap.Modal(cartModalElement) : null;

    const orderConfirmationModalElement = document.getElementById('orderConfirmationModal');
    const orderConfirmationModal = orderConfirmationModalElement ? new bootstrap.Modal(orderConfirmationModalElement) : null;

    const orderCompleteModalElement = document.getElementById('orderCompleteModal');
    const orderCompleteModal = orderCompleteModalElement ? new bootstrap.Modal(orderCompleteModalElement) : null;

    // --- グローバルデータ ---
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

    // --- ヘルパー関数 ---
    /**
     * APIエラーを処理し、アラートを表示します。
     * @param {Response} response - fetch APIのResponseオブジェクト
     * @param {string} defaultMessage - エラーが発生した場合に表示するデフォルトメッセージ
     * @returns {Promise<Error>} - 処理されたエラーオブジェクト
     */
    async function handleError(response, defaultMessage) {
        let errorMessage = defaultMessage;
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || defaultMessage;
        } catch (e) {
            // JSONパースに失敗した場合（例: サーバーがHTMLや空の応答を返した場合）
            console.warn("Error parsing response body as JSON:", e);
        }
        console.error('Error:', errorMessage);
        alert(errorMessage);
        throw new Error(errorMessage); // 後続のcatchブロックで捕捉できるようにエラーを再スロー
    }

    /**
     * 指定されたモーダルを表示または非表示にします。
     * @param {bootstrap.Modal} modalInstance - Bootstrapモーダルのインスタンス
     * @param {boolean} show - trueなら表示、falseなら非表示
     */
    function toggleModal(modalInstance, show) {
        if (modalInstance) {
            if (show) {
                modalInstance.show();
            } else {
                modalInstance.hide();
            }
        } else {
            console.warn("Modal instance is null. Cannot perform toggle operation.");
        }
    }

    /**
     * ヘッダーの右側ボタン部分をログイン状態に応じて更新する
     * @param {boolean} loggedIn - ログイン状態
     * @param {string} [userName=''] - ユーザー名 (ログイン時のみ)
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
                            currentOrderData.customerInfo.customerId = null;
                            alert("ログアウトしました");
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

    // --- 商品表示ロジック ---
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

        const filteredProducts = allProducts.filter(product => {
            const matchesCategory = currentSelectedCategory === 'all' || product.categoryId.toString() === currentSelectedCategory;
            const matchesSearchTerm = product.name.toLowerCase().includes(currentSearchTerm) ||
                                      (product.description && product.description.toLowerCase().includes(currentSearchTerm));
            return matchesCategory && matchesSearchTerm;
        });


        if (filteredProducts.length === 0) {
            container.innerHTML = '<p class="text-center">該当する商品が見つかりませんでした。</p>';
            return;
        }

        container.innerHTML = filteredProducts.map(product => `
            <div class="col">
                <div class="card product-card" data-category="${product.categoryName}">
                    <img src="${product.imageUrl || 'https://via.placeholder.com/300x200'}" class="card-img-top" alt="${product.name}">
                    <div class="card-body">
                        <h5 class="card-title">${product.name}</h5>
                        <p class="card-text">¥${product.price.toLocaleString()}</p>
                        <p class="card-text" style="color: gray;">${product.categoryName}</p>
                        <button class="btn btn-outline-primary view-product" data-id="${product.productId}">詳細を見る</button>
                    </div>
                </div>
            </div>
        `).join('');

        container.querySelectorAll('.view-product').forEach(button => {
            button.addEventListener('click', function() {
                fetchProductDetail(this.dataset.id);
            });
        });
    }


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

    // --- カートモーダル表示・更新ロジック ---
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
            // --- 顧客情報入力フォーム表示 ---
            modalTitle.textContent = 'お客様情報入力';
            modalBody.innerHTML = `
                <form id="order-form" class="needs-validation" novalidate>
                    <div class="mb-3">
                        <label for="name" class="form-label">お名前(全角)</label>
                        <input type="text" class="form-control" id="name" required>
                        <div class="invalid-feedback" id="name-feedback">お名前は2文字以上の全角で入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">メールアドレス(半角)</label>
                        <input type="email" class="form-control" id="email" required>
                        <div class="invalid-feedback" id="email-feedback">有効なメールアドレスを半角で入力してください (例: user@example.com)</div>
                    </div>
                    <div class="mb-3">
                        <label for="address" class="form-label">住所(全角)</label>
                        <input type="text" class="form-control" id="address" required>
                        <div class="invalid-feedback" id="address-feedback">住所は5文字以上の全角で入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="phone" class="form-label">電話番号(半角)</label>
                        <input type="tel" class="form-control" id="phone" required pattern="^0\\d{9,10}$">
                        <div class="invalid-feedback" id="phone-feedback">有効な電話番号を半角で入力してください (ハイフンなし、0から始まる10桁または11桁)</div>
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
            
            // --- 顧客情報入力フォームの全角・半角バリデーションと初期値設定 ---
            const nameInput = document.getElementById('name');
            const emailInput = document.getElementById('email');
            const addressInput = document.getElementById('address');
            const phoneInput = document.getElementById('phone');

            const nameFeedback = document.getElementById('name-feedback');
            const emailFeedback = document.getElementById('email-feedback');
            const addressFeedback = document.getElementById('address-feedback');
            const phoneFeedback = document.getElementById('phone-feedback');

            /**
             * 文字列が全て全角文字であるかをチェックします。
             * @param {string} str - チェックする文字列
             * @returns {boolean} - 全て全角文字であれば true、そうでなければ false
             */
            function isFullWidth(str) {
                if (!str) return true;
                return !/[ -~｡-ﾟ]/.test(str) &&
                       str.split('').every(char => {
                           const code = char.charCodeAt(0);
                           return (code >= 0xFF01 && code <= 0xFF5E) ||
                                  (code >= 0x3040 && code <= 0x309F) ||
                                  (code >= 0x30A0 && code <= 0x30FF) ||
                                  (code >= 0x4E00 && code <= 0x9FFF) ||
                                  (code === 0x3000);
                       });
            }

            /**
             * 文字列が全て半角文字（半角英数字、半角記号、半角スペース）であるかをチェックします。
             * @param {string} str - チェックする文字列
             * @returns {boolean} - 全て半角文字であれば true、そうでなければ false
             */
            function isHalfWidth(str) {
                if (!str) return true; // 空文字列は半角とみなす
                // 半角英数字と一般的な半角記号、半角スペースを許可する正規表現
                // ここにはメールアドレスでよく使われる記号（@ . - _ +）なども含まれるべき
                return /^[ -~｡-ﾟ]*$/.test(str); 
            }
            // お名前 (全角)
            if (nameInput) {
                nameInput.addEventListener('input', function() {
                    if (this.value.length < 2) {
                        this.setCustomValidity('お名前は2文字以上で入力してください');
                        nameFeedback.textContent = 'お名前は2文字以上で入力してください';
                    } else if (!isFullWidth(this.value)) {
                        this.setCustomValidity('お名前は全角で入力してください');
                        nameFeedback.textContent = 'お名前は全角で入力してください';
                    } else {
                        this.setCustomValidity('');
                    }
                    this.reportValidity();
                });
                nameInput.addEventListener('blur', function() {
                    nameInput.dispatchEvent(new Event('input'));
                });
            }

            // メールアドレス (半角)
            if (emailInput) {
                emailInput.addEventListener('input', function() {
                    if (!isHalfWidth(this.value)) {
                        this.setCustomValidity('メールアドレスは半角で入力してください');
                        emailFeedback.textContent = 'メールアドレスは半角で入力してください';
                    } else if (!this.checkValidity()) {
                        this.setCustomValidity('有効なメールアドレス形式で入力してください (例: user@example.com)');
                        emailFeedback.textContent = '有効なメールアドレス形式で入力してください (例: user@example.com)';
                    } else {
                        this.setCustomValidity('');
                    }
                    this.reportValidity();
                });
                emailInput.addEventListener('blur', function() {
                    emailInput.dispatchEvent(new Event('input'));
                });
            }

            // 住所 (全角)
            if (addressInput) {
                addressInput.addEventListener('input', function() {
                    if (this.value.length < 5) {
                        this.setCustomValidity('住所は5文字以上で入力してください');
                        addressFeedback.textContent = '住所は5文字以上で入力してください';
                    } else if (!isFullWidth(this.value)) {
                        this.setCustomValidity('住所は全角で入力してください');
                        addressFeedback.textContent = '住所は全角で入力してください';
                    } else {
                        this.setCustomValidity('');
                    }
                    this.reportValidity();
                });
                addressInput.addEventListener('blur', function() {
                    addressInput.dispatchEvent(new Event('input'));
                });
            }

            // 電話番号 (半角)
            if (phoneInput) {
                phoneInput.addEventListener('input', function() {
                    if (!isHalfWidth(this.value)) {
                        this.setCustomValidity('電話番号は半角で入力してください');
                        phoneFeedback.textContent = '電話番号は半角で入力してください';
                    } else if (!this.checkValidity()) {
                        this.setCustomValidity('有効な電話番号を半角で入力してください (ハイフンなし、0から始まる10桁または11桁)');
                        phoneFeedback.textContent = '有効な電話番号を半角で入力してください (ハイフンなし、0から始まる10桁または11桁)';
                    } else {
                        this.setCustomValidity('');
                    }
                    this.reportValidity();
                });
                phoneInput.addEventListener('blur', function() {
                    phoneInput.dispatchEvent(new Event('input'));
                });
            }


            // ログイン済みの場合は顧客情報をフォームに自動入力
            const customer = await fetchLoggedInCustomerInfo();
            if (customer) {
                if (nameInput) nameInput.value = customer.name || '';
                if (emailInput) emailInput.value = customer.email || '';
                if (addressInput) addressInput.value = customer.address || '';
                if (phoneInput) phoneInput.value = customer.phoneNumber || '';
                // 自動入力後、バリデーションを一度実行して表示を更新
                nameInput?.dispatchEvent(new Event('input'));
                emailInput?.dispatchEvent(new Event('input'));
                addressInput?.dispatchEvent(new Event('input'));
                phoneInput?.dispatchEvent(new Event('input'));

            } else {
                // 未ログインの場合は既存のcurrentOrderDataから復元
                if (nameInput) nameInput.value = currentOrderData.customerInfo.name || '';
                if (emailInput) emailInput.value = currentOrderData.customerInfo.email || '';
                if (addressInput) addressInput.value = currentOrderData.customerInfo.address || '';
                if (phoneInput) phoneInput.value = currentOrderData.customerInfo.phoneNumber || '';
            }
            // 支払い方法のラジオボタンの状態を復元
            if (currentOrderData.paymentMethod) {
                const radio = document.querySelector(`input[name="paymentMethod"][value="${currentOrderData.paymentMethod}"]`);
                if (radio) radio.checked = true;
            }

            // Bootstrapのカスタムバリデーションクラスを適用 (初期表示時)
            const form = document.getElementById('order-form');
            if (form) { 
                // 支払い方法ラジオボタンのバリデーション表示制御
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
                    // 初期状態のバリデーション表示
                    if (document.querySelector('input[name="paymentMethod"]:checked')) {
                        paymentMethodFeedback.style.display = 'none';
                    } else {
                        paymentMethodFeedback.style.display = 'block';
                    }
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
            await updateCartModalContent();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('商品数量更新エラー:', error);
            await updateCartModalContent(); 
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
            await updateCartModalContent();
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

        // フォーム全体のバリデーションを実行
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

        // フォームデータからcurrentOrderDataを更新
        const nameInput = document.getElementById('name');
        const emailInput = document.getElementById('email');
        const addressInput = document.getElementById('address');
        const phoneInput = document.getElementById('phone');
        const existingCustomerId = currentOrderData.customerInfo.customerId;

        currentOrderData.customerInfo = {
            customerId: existingCustomerId,
            name: nameInput ? nameInput.value : '',
            email: emailInput ? emailInput.value : '',
            address: addressInput ? addressInput.value : '',
            phoneNumber: phoneInput ? phoneInput.value : ''
        };
        currentOrderData.paymentMethod = paymentMethodElement ? paymentMethodElement.value : '';

        // カートの内容を再取得し、注文データにセット
        try {
            const cartResponse = await fetch(`${API_BASE}/cart`);
            if (!cartResponse.ok) {
                await handleError(cartResponse, 'カート内容の取得に失敗しました。');
                return;
            }
            const cart = await cartResponse.json();
            currentOrderData.items = Object.values(cart.items).map(item => ({
                productId: item.id,
                quantity: item.quantity,
                priceAtOrder: item.price
            }));
            currentOrderData.totalPrice = cart.grandTotal;

            // 注文確認モーダルを表示
            showOrderConfirmationModal(currentOrderData);
        } catch (error) {
            console.error('注文内容確認のためのカート情報取得エラー:', error);
            alert('注文内容の準備中にエラーが発生しました。もう一度お試しください。');
        }
    }

    /**
     * 注文確認モーダルを表示します。
     * @param {object} orderData - 注文データ
     */
    function showOrderConfirmationModal(orderData) {
        const orderConfirmationModalTitle = document.getElementById('orderConfirmationModalTitle');
        const orderConfirmationModalBody = document.getElementById('orderConfirmationModalBody');
        const orderConfirmationModalFooter = document.getElementById('orderConfirmationModalFooter');

        if (!orderConfirmationModalTitle || !orderConfirmationModalBody || !orderConfirmationModalFooter) {
            console.warn("Order confirmation modal elements not found.");
            return;
        }

        orderConfirmationModalTitle.textContent = '注文内容確認';

        let itemsHtml = orderData.items.map(item => {
            const product = allProducts.find(p => p.productId === item.productId);
            const productName = product ? product.name : '不明な商品';
            return `<li>${productName} x ${item.quantity}個 (単価: ¥${item.priceAtOrder.toLocaleString()})</li>`;
        }).join('');

        let customerInfoHtml = `
            <p><strong>お名前:</strong> ${orderData.customerInfo.name}</p>
            <p><strong>メールアドレス:</strong> ${orderData.customerInfo.email}</p>
            <p><strong>住所:</strong> ${orderData.customerInfo.address}</p>
            <p><strong>電話番号:</strong> ${orderData.customerInfo.phoneNumber}</p>
        `;

        const paymentMethodText = orderData.paymentMethod === 'bank_transfer' ? '銀行振込' : '代金引換';

        orderConfirmationModalBody.innerHTML = `
            <h6>お客様情報:</h6>
            ${customerInfoHtml}
            <h6>ご注文商品:</h6>
            <ul>${itemsHtml}</ul>
            <p><strong>お支払い方法:</strong> ${paymentMethodText}</p>
            <hr>
            <h5 class="text-end">合計金額: ¥${orderData.totalPrice.toLocaleString()}</h5>
        `;

        orderConfirmationModalFooter.innerHTML = `
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">修正する</button>
            <button type="button" class="btn btn-success" id="confirm-order-btn">注文を確定する</button>
        `;

        const confirmOrderBtn = document.getElementById('confirm-order-btn');
        if (confirmOrderBtn) {
            confirmOrderBtn.addEventListener('click', confirmOrder);
        }

        toggleModal(cartModal, false); // カートモーダルを閉じる
        toggleModal(orderConfirmationModal, true); // 注文確認モーダルを表示
    }

    /**
     * 注文を確定し、サーバーに送信します。
     */
    async function confirmOrder() {
        try {
            const response = await fetch(`${API_BASE}/orders`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(currentOrderData)
            });

            if (!response.ok) {
                const errorBody = await response.text();
                console.error("Order confirmation failed. Status:", response.status, "Body:", errorBody);
                try {
                    const errorData = JSON.parse(errorBody);
                    await handleError(response, errorData.message || '注文の確定に失敗しました');
                } catch (jsonError) {
                    await handleError(response, '注文の確定に失敗しました (サーバーからのメッセージを読み取れませんでした)。');
                }
                return;
            }

            const result = await response.json();
            alert('ご注文が確定しました！注文番号: ' + result.orderId);
            currentOrderData.items = []; // カートをクリア
            currentOrderData.totalPrice = 0;
            updateCartBadge(0); // バッジを0に
            toggleModal(orderConfirmationModal, false); // 確認モーダルを閉じる
            toggleModal(orderCompleteModal, true); // 完了モーダルを表示

        } catch (error) {
            console.error('注文確定エラー:', error.message);
        }
    }
});