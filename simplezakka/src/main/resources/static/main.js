document.addEventListener('DOMContentLoaded', function() {
    const productModal = new bootstrap.Modal(document.getElementById('productModal'));
    const cartModal = new bootstrap.Modal(document.getElementById('cartModal'));
    const orderConfirmationModal = new bootstrap.Modal(document.getElementById('orderConfirmationModal'));
    const orderCompleteModal = new bootstrap.Modal(document.getElementById('orderCompleteModal'));

    // APIのベースURL。開発環境ではlocalhost、デプロイ時は相対パスを使用するなど調整してください。
    const API_BASE = 'http://localhost:8080/api';

    // 注文処理全体で共有するデータ構造
    let currentOrderData = {
        customerInfo: {
            name: '',
            email: '',
            address: '',
            phoneNumber: ''
        },
        paymentMethod: '',
        items: [],
        totalPrice: 0
    };

    // 共通のエラーハンドリング関数
    async function handleError(response, defaultMessage) {
        let errorMessage = defaultMessage;
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || defaultMessage;
        } catch (e) {
            // JSON解析エラーの場合はデフォルトメッセージを使用
        }
        console.error('Error:', errorMessage);
        alert(errorMessage);
        throw new Error(errorMessage); // 後続の処理を中断するためthrowする
    }

    // 汎用的なモーダル表示/非表示関数
    function toggleModal(modalInstance, show) {
        if (show) {
            modalInstance.show();
        } else {
            modalInstance.hide();
        }
    }


    /**
     * ヘッダーの右側ボタン部分をログイン状態に応じて更新する
     * @param {boolean} loggedIn - ログイン状態 (true: ログイン済み, false: 未ログイン)
     * @param {string} [userName=''] - ログインユーザー名 (loggedInがtrueの場合のみ使用)
     */
    async function updateHeaderButtons(loggedIn, userName = '') {
        const headerRightButtons = document.getElementById("header-right-buttons");
        if (!headerRightButtons) {
            console.error("Header right buttons container not found!");
            return;
        }

        let buttonsHtml = '';
        if (loggedIn) {
            // ログイン中の場合
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
            // ログインしていない場合
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

        // ボタンが動的に追加された後、イベントリスナーを再設定
        const cartBtn = document.getElementById("cart-btn");
        if (cartBtn) {
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
                            window.location.reload(); // ログアウト成功後、ページをリロードして未ログイン状態にする
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
        // カートバッジの初期更新
        updateCartDisplay();
    }

    async function initializeHeader() {
        try {
            const response = await fetch('/api/customers/status');
            const data = await response.json();

            if (response.ok && data.loggedIn) {
                updateHeaderButtons(true, data.customerName);
            } else {
                updateHeaderButtons(false);
            }
        } catch (error) {
            console.error('ログイン状態確認エラー:', error);
            // エラーが発生した場合も未ログイン状態として表示
            updateHeaderButtons(false);
        }
    }

    // ページロード時の初期化処理
    initializeHeader(); // ヘッダーの初期化をここで行う
    fetchProducts(); // 商品リストの取得


    // 検索機能の追加 (変更なし)
    document.getElementById('searchInput').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        const products = document.querySelectorAll('#products-container .col');

        products.forEach(product => {
            const productName = product.querySelector('.card-title').textContent.toLowerCase();
            const productDescription = product.querySelector('.card-text').textContent.toLowerCase();
            if (productName.includes(searchTerm) || productDescription.includes(searchTerm)) {
                product.style.display = 'block';
            } else {
                product.style.display = 'none';
            }
        });
    });

    // カテゴリーフィルターのイベントリスナー設定
    document.querySelectorAll('.category-btn').forEach(button => {
        button.addEventListener('click', function() {
            const category = this.dataset.category;
            filterProductsByCategory(category);
        });
    });

    function filterProductsByCategory(category) {
        const products = document.querySelectorAll('#products-container .col');
        products.forEach(product => {
            const productCategory = product.querySelector('.product-card').dataset.category; // カテゴリー情報がHTMLにないため、後で追加が必要です
            if (category === 'all' || productCategory === category) {
                product.style.display = 'block';
            } else {
                product.style.display = 'none';
            }
        });
    }


    async function fetchProducts() {
        try {
            const response = await fetch(`${API_BASE}/products`);
            if (!response.ok) {
                await handleError(response, '商品の取得に失敗しました');
            }
            const products = await response.json();
            displayProducts(products);
        } catch (error) {
            console.error(error.message);
        }
    }

    function displayProducts(products) {
        const container = document.getElementById('products-container');
        container.innerHTML = products.map(product => `
            <div class="col">
                <div class="card product-card" data-category="${product.category}"> <img src="${product.imageUrl || 'https://via.placeholder.com/300x200'}" class="card-img-top" alt="${product.name}">
                    <div class="card-body">
                        <h5 class="card-title">${product.name}</h5>
                        <p class="card-text">¥${product.price.toLocaleString()}</p>
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
        document.getElementById('productModalTitle').textContent = product.name;
        const modalBody = document.getElementById('productModalBody');
        modalBody.innerHTML = `
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

        modalBody.querySelector('.add-to-cart').addEventListener('click', function() {
            const quantityInput = document.getElementById('quantity');
            const quantity = parseInt(quantityInput.value);
            const stock = parseInt(document.getElementById('product-stock').textContent);

            // 在庫数と入力数量のバリデーション
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
            method: 'GET', // 明示的にGETメソッドを指定しても良い
            headers: {
                'Content-Type': 'application/json'
                // 他のヘッダーが必要であればここに追加
            },
            // ★この行が最も重要です★
            // これにより、ブラウザはセッションCookie（JSESSIONIDなど）をリクエストに含めます。
            credentials: 'include' 
        });

        if (response.ok) {
            const customer = await response.json();
            console.log("Fetched logged-in customer data:", customer); // デバッグ用
            return customer;
        } else if (response.status === 401) { // 未認証の場合
            console.log("User is not logged in or session expired (401 Unauthorized).");
            return null;
        } else {
            // その他のエラー (例: 500 Internal Server Error, 404 Not Found)
            const errorData = await response.json().catch(() => ({ message: '不明なエラー' })); // JSONパース失敗も考慮
            console.error(`Failed to fetch customer info: ${response.status} - ${errorData.message}`);
            // handleError 関数があればここで呼び出すことも検討
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
        await updateCartModalContent();
        toggleModal(cartModal, true);
    }

    async function updateCartModalContent(showCheckoutForm = false) {
        const modalTitle = document.getElementById('cartModalTitle');
        const modalBody = document.getElementById('cartModalBody');
        const modalFooter = document.getElementById('cartModalFooter');

        if (!showCheckoutForm) {
            modalTitle.textContent = 'ショッピングカート';
            try {
                const response = await fetch(`${API_BASE}/cart`);
                if (!response.ok) {
                    await handleError(response, 'カート情報の取得に失敗しました');
                }
                const cart = await response.json();

                // カートが空の場合はメッセージを表示
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

                    // 数量更新イベントの設定
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
                    document.getElementById('proceed-to-checkout-form').addEventListener('click', () => updateCartModalContent(true));
                    const loginToPurchaseBtn = document.getElementById('login-to-purchase-btn');
                    if (loginToPurchaseBtn) {
                        loginToPurchaseBtn.addEventListener('click', async () => {
                            try {
                                const statusResponse = await fetch('/api/customers/status');
                                const statusData = await statusResponse.json();
                                if (statusResponse.ok && statusData.loggedIn) {
                                    alert('すでにログインしています。注文手続きへ進みます。');
                                    updateCartModalContent(true); // ログイン済みなら直接注文フォームへ
                                } else {
                                    window.location.href = 'C0601.html'; // 未ログインならログインページへ
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
            document.getElementById('back-to-cart').addEventListener('click', () => updateCartModalContent(false));
            document.getElementById('submit-order-form-and-show-confirmation').addEventListener('click', submitOrderFormAndShowConfirmation);
            const customer = await fetchLoggedInCustomerInfo();
            if (customer) {
                // ログインしているユーザーの情報があればフォームにセット
                document.getElementById('name').value = customer.name || '';
                document.getElementById('email').value = customer.email || '';
                document.getElementById('address').value = customer.address || '';
                // バックエンドのプロパティ名に合わせて 'phoneNumber' または 'phone' などを調整してください
                document.getElementById('phone').value = customer.phoneNumber || customer.phone || ''; 
            } else {
                // ログインしていない場合、または情報取得に失敗した場合は
                // 既存の currentOrderData に保持されている値をセット（ユーザーが手動で入力していた場合など）
                document.getElementById('name').value = currentOrderData.customerInfo.name || '';
                document.getElementById('email').value = currentOrderData.customerInfo.email || '';
                document.getElementById('address').value = currentOrderData.customerInfo.address || '';
                document.getElementById('phone').value = currentOrderData.customerInfo.phoneNumber || '';
            }
            if (currentOrderData.paymentMethod) {
                const radio = document.querySelector(`input[name="paymentMethod"][value="${currentOrderData.paymentMethod}"]`);
                if (radio) radio.checked = true;
            }
            const form = document.getElementById('order-form');
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
            const paymentRadios = document.querySelectorAll('input[name="paymentMethod"]');
            const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
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
                paymentMethodFeedback.style.display = 'block'; // デフォルトで表示
            }
        }
    }

    async function updateItemQuantity(itemId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity: parseInt(quantity) })
            });

            if (!response.ok) {
                await handleError(response, '数量の更新に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error(error.message);
            updateCartModalContent(); // 失敗時は元の状態に戻す
        }
    }

    async function removeItem(itemId) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                await handleError(response, '商品の削除に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error(error.message);
        }
    }

    async function submitOrderFormAndShowConfirmation() {
        const form = document.getElementById('order-form');

        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            form.querySelector(':invalid')?.focus();
            return;
        }

        const paymentMethodElement = document.querySelector('input[name="paymentMethod"]:checked');
        const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
        if (!paymentMethodElement) {
            paymentMethodFeedback.style.display = 'block';
            alert('決済方法を選択してください。');
            return;
        } else {
            paymentMethodFeedback.style.display = 'none';
        }

        currentOrderData.customerInfo = {
            name: document.getElementById('name').value,
            email: document.getElementById('email').value,
            address: document.getElementById('address').value,
            phoneNumber: document.getElementById('phone').value
        };

        currentOrderData.paymentMethod = paymentMethodElement.value;

        try {
            const cartResponse = await fetch(`${API_BASE}/cart`);
            if (!cartResponse.ok) {
                const errorData = await cartResponse.json();
                throw new Error(errorData.message || 'カート情報の取得に失敗しました');
            }
            const cart = await cartResponse.json();

            currentOrderData.items = Object.values(cart.items);
            currentOrderData.totalPrice = cart.totalPrice;

            toggleModal(cartModal, false);
            showOrderConfirmation();
        } catch (error) {
            console.error('Error preparing order confirmation:', error);
            alert(`注文情報の準備中にエラーが発生しました: ${error.message}`);
        }
    }

    function showOrderConfirmation() {
        document.getElementById('orderConfirmationModalTitle').textContent = '注文内容の確認';
        const modalBody = document.getElementById('orderConfirmationModalBody');
        const modalFooter = document.getElementById('orderConfirmationModalFooter');

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
        if (currentOrderData.items.length > 0) {
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
                            <th colspan="3" class="text-end">合計金額:</th>
                            <th>¥${currentOrderData.totalPrice.toLocaleString()}</th>
                        </tr>
                    </tfoot>
                </table>
            </div>
        `;

        const customerInfo = currentOrderData.customerInfo;
        let customerHtml = `
            <div class="confirmation-box">
                <div class="section-title">お届け先</div>
                <dl class="row">
                    <dt class="col-sm-4">お名前</dt>
                    <dd class="col-sm-8">${customerInfo.name || ''}</dd>

                    <dt class="col-sm-4">メールアドレス</dt>
                    <dd class="col-sm-8">${customerInfo.email || ''}</dd>

                    <dt class="col-sm-4">住所</dt>
                    <dd class="col-sm-8">${customerInfo.address || ''}</dd>

                    <dt class="col-sm-4">電話番号</dt>
                    <dd class="col-sm-8">${customerInfo.phoneNumber || ''}</dd>
                </dl>
            </div>
        `;

        let paymentHtml = `
            <div class="confirmation-box">
                <div class="section-title">お支払い方法</div>
                <p>${currentOrderData.paymentMethod === 'bank_transfer' ? '銀行振込' :
                      currentOrderData.paymentMethod === 'cash_on_delivery' ? '代金引換' :
                      currentOrderData.paymentMethod || ''}</p>
            </div>
        `;

        modalBody.innerHTML = itemsHtml + customerHtml + paymentHtml;

        modalFooter.innerHTML = `
            <button type="button" class="btn btn-secondary" id="back-to-customer-form">戻る</button>
            <button type="button" class="btn btn-primary" id="final-confirm-order-btn">注文を確定する</button>
        `;
        
        document.getElementById('back-to-customer-form').addEventListener('click', function() {
            toggleModal(orderConfirmationModal, false);
            toggleModal(cartModal, true);
            updateCartModalContent(true);
        });

        document.getElementById('final-confirm-order-btn').addEventListener('click', confirmOrder);

        toggleModal(orderConfirmationModal, true);
    }

    async function confirmOrder() {
        try {
            const response = await fetch(`${API_BASE}/order/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(currentOrderData)
            });

            if (!response.ok) {
                await handleError(response, '注文の確定に失敗しました');
            }

            const orderResult = await response.json();

            await fetch(`${API_BASE}/cart`, { method: 'DELETE' });
            updateCartBadge(0);

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
                    name: '', email: '', address: '', phoneNumber: ''
                },
                paymentMethod: '',
                items: [],
                totalPrice: 0
            };

            toggleModal(orderConfirmationModal, false);
            displayOrderComplete(orderResult);
            toggleModal(orderCompleteModal, true);

        } catch (error) {
            console.error('Error confirming order:', error);
            alert(`注文確定中にエラーが発生しました。もう一度お試しください: ${error.message}`);
        }
    }

    function displayOrderComplete(order) {
        document.getElementById('orderCompleteModalTitle').textContent = 'ご注文完了';
        const modalBody = document.getElementById('orderCompleteModalBody');

        const displayPaymentMethod = (order.paymentMethod === 'bank_transfer') ? '銀行振込' :
                                     (order.paymentMethod === 'cash_on_delivery') ? '代金引換' :
                                     order.paymentMethod || '';
        modalBody.innerHTML = `
            <p>ご注文ありがとうございます。注文番号は <strong>${order.orderId}</strong> です。</p>
            <p>ご注文日時: ${new Date(order.orderDate).toLocaleString()}</p>
            <p>決済方法: ${displayPaymentMethod}</p>
            <p>商品合計: ¥${order.totalPrice.toLocaleString()}</p>
            <p>送料: ¥${order.shippingFee.toLocaleString()}</p>
            <p class="fs-5">最終お支払い金額: ¥${order.grandTotal.toLocaleString()}</p>
        `;
        const modalFooter = document.getElementById('orderCompleteModalFooter');
        modalFooter.innerHTML = `<button type="button" class="btn btn-primary" data-bs-dismiss="modal">閉じる</button>`;
    }

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('showCart') === 'true' && urlParams.get('showCheckoutForm') === 'true') {
        cartModal.show();
        updateCartModalContent(true); 
        history.replaceState({}, document.title, window.location.pathname);
    }
});