document.addEventListener('DOMContentLoaded', function() {
    // モーダル要素の取得
    const productModal = new bootstrap.Modal(document.getElementById('productModal'));
    const cartModal = new bootstrap.Modal(document.getElementById('cartModal'));
    // 注文確認モーダルを再導入
    const orderConfirmationModal = new bootstrap.Modal(document.getElementById('orderConfirmationModal'));
    const orderCompleteModal = new bootstrap.Modal(document.getElementById('orderCompleteModal'));

    // APIのベースURL
    const API_BASE = '/api';

    // 注文処理全体で共有するデータ構造
    // お客様情報とカート情報を一時的に保持します
    let currentOrderData = {
        customerInfo: {
            name: '',
            email: '',
            address: '',
            phoneNumber: '',
            paymentMethod: ''
        },
        items: [],
        totalPrice: 0
    };

    // 商品一覧の取得と表示
    fetchProducts();

    // カート情報の取得と表示
    updateCartDisplay();

    // カートボタンクリックイベント
    document.getElementById('cart-btn').addEventListener('click', function() {
        showCartModal();
    });

    // 商品一覧を取得して表示する関数
    async function fetchProducts() {
        try {
            const response = await fetch(`${API_BASE}/products`);
            if (!response.ok) {
                throw new Error('商品の取得に失敗しました');
            }
            const products = await response.json();
            displayProducts(products);
        } catch (error) {
            console.error('Error:', error);
            alert('商品の読み込みに失敗しました');
        }
    }

    // 商品一覧を表示する関数
    function displayProducts(products) {
        const container = document.getElementById('products-container');
        container.innerHTML = '';

        products.forEach(product => {
            const card = document.createElement('div');
            card.className = 'col';
            card.innerHTML = `
                <div class="card product-card">
                    <img src="${product.imageUrl || 'https://via.placeholder.com/300x200'}" class="card-img-top" alt="${product.name}">
                    <div class="card-body">
                        <h5 class="card-title">${product.name}</h5>
                        <p class="card-text">¥${product.price.toLocaleString()}</p>
                        <button class="btn btn-outline-primary view-product" data-id="${product.productId}">詳細を見る</button>
                    </div>
                </div>
            `;
            container.appendChild(card);

            // 詳細ボタンのイベント設定
            card.querySelector('.view-product').addEventListener('click', function() {
                fetchProductDetail(product.productId);
            });
        });
    }

    // 商品詳細を取得する関数
    async function fetchProductDetail(productId) {
        try {
            const response = await fetch(`${API_BASE}/products/${productId}`);
            if (!response.ok) {
                throw new Error('商品詳細の取得に失敗しました');
            }
            const product = await response.json();
            displayProductDetail(product);
        } catch (error) {
            console.error('Error:', error);
            alert('商品詳細の読み込みに失敗しました');
        }
    }

    // 商品詳細を表示する関数
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
                        <label for="quantity" class="me-2">数量:</label>
                        <input type="number" id="quantity" class="form-control w-25" value="1" min="1" max="${product.stock}">
                    </div>
                    <button class="btn btn-primary add-to-cart" data-id="${product.productId}">カートに入れる</button>
                </div>
            </div>
        `;

        // カートに追加ボタンのイベント設定
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

        productModal.show();
    }

    // カートに商品を追加する関数
    async function addToCart(productId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    productId: productId,
                    quantity: quantity
                })
            });

            if (!response.ok) {
                // エラーレスポンスのパースを試みる
                const errorData = await response.json();
                throw new Error(errorData.message || 'カートへの追加に失敗しました');
            }

            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);

            productModal.hide();
            alert('商品をカートに追加しました');
        } catch (error) {
            console.error('Error:', error);
            alert(`カートへの追加に失敗しました: ${error.message}`);
        }
    }

    // カート情報を取得する関数 (バッジ更新用)
    async function updateCartDisplay() {
        try {
            const response = await fetch(`${API_BASE}/cart`);
            if (!response.ok) {
                throw new Error('カート情報の取得に失敗しました');
            }
            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
        }
    }

    // カートバッジを更新する関数
    function updateCartBadge(count) {
        document.getElementById('cart-count').textContent = count;
    }

    // カートモーダルを表示する関数
    async function showCartModal() {
        await updateCartModalContent(); // 最新のカート情報を取得して表示
        cartModal.show();
    }

    // カートモーダルの内容を更新する関数 (カート表示と注文フォームの切り替えを内包)
    async function updateCartModalContent(showCheckoutForm = false) {
        const modalTitle = document.getElementById('cartModalTitle');
        const modalBody = document.getElementById('cartModalBody');
        const modalFooter = document.getElementById('cartModalFooter');

        if (!showCheckoutForm) {
            // カート内容の表示
            modalTitle.textContent = 'ショッピングカート';
            try {
                const response = await fetch(`${API_BASE}/cart`);
                if (!response.ok) {
                    throw new Error('カート情報の取得に失敗しました');
                }
                const cart = await response.json();
                
                // カートが空の場合はメッセージを表示
                if (cart.items && Object.keys(cart.items).length > 0) {
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
                                    <th colspan="3" class="text-end">合計:</th>
                                    <th>¥${cart.totalPrice.toLocaleString()}</th>
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

                    // 削除ボタンイベントの設定
                    document.querySelectorAll('.remove-item').forEach(button => {
                        button.addEventListener('click', function() {
                            removeItem(this.dataset.id);
                        });
                    });

                    // フッターのボタン設定
                    modalFooter.innerHTML = `
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">買い物を続ける</button>
                        <button type="button" class="btn btn-primary" id="proceed-to-checkout-form">注文手続きへ</button>
                    `;
                    document.getElementById('proceed-to-checkout-form').addEventListener('click', () => updateCartModalContent(true));

                } else {
                    modalBody.innerHTML = '<p class="text-center">カートは空です</p>';
                    modalFooter.innerHTML = `<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">閉じる</button>`;
                }
            } catch (error) {
                console.error('Error:', error);
                alert('カート情報の読み込みに失敗しました');
                modalBody.innerHTML = '<p class="text-center text-danger">カート情報の読み込みに失敗しました。</p>';
                modalFooter.innerHTML = `<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">閉じる</button>`;
            }
        } else {
            // 注文フォームの表示
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
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentBankTransfer" value="銀行振込" required>
                            <label class="form-check-label" for="paymentBankTransfer">
                                銀行振込
                            </label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentCashOnDelivery" value="代金引換" required>
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

            // 以前の入力内容を復元
            document.getElementById('name').value = currentOrderData.customerInfo.name;
            document.getElementById('email').value = currentOrderData.customerInfo.email;
            document.getElementById('address').value = currentOrderData.customerInfo.address;
            document.getElementById('phone').value = currentOrderData.customerInfo.phoneNumber;
            if (currentOrderData.customerInfo.paymentMethod) {
                const radio = document.querySelector(`input[name="paymentMethod"][value="${currentOrderData.customerInfo.paymentMethod}"]`);
                if (radio) radio.checked = true;
            }

            // バリデーションイベントリスナーを追加
            const form = document.getElementById('order-form');
            form.querySelectorAll('input, select').forEach(input => {
                input.addEventListener('input', () => {
                    if (input.checkValidity()) {
                        input.classList.remove('is-invalid');
                        input.classList.add('is-valid');
                    } else {
                        input.classList.remove('is-valid');
                        input.classList.add('is-invalid');
                    }
                });
                input.addEventListener('blur', () => { // フォーカスが外れたときにもチェック
                    if (input.checkValidity()) {
                        input.classList.remove('is-invalid');
                        input.classList.add('is-valid');
                    } else {
                        input.classList.remove('is-valid');
                        input.classList.add('is-invalid');
                    }
                });
            });

            // 決済方法のラジオボタンに対するバリデーション表示
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
            // 初期表示時にも決済方法が選択されているかチェック
            if (document.querySelector('input[name="paymentMethod"]:checked')) {
                paymentMethodFeedback.style.display = 'none';
            } else {
                paymentMethodFeedback.style.display = 'block'; // デフォルトで表示
            }
        }
    }

    // カート内の商品数量を更新する関数
    async function updateItemQuantity(itemId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    quantity: parseInt(quantity)
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '数量の更新に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
            alert(`数量の更新に失敗しました: ${error.message}`);
            updateCartModalContent(); // 失敗時は元の状態に戻す
        }
    }

    // カート内の商品を削除する関数
    async function removeItem(itemId) {
        try {
            const response = await fetch(`${API_BASE}/cart/items/${itemId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '商品の削除に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
            alert(`商品の削除に失敗しました: ${error.message}`);
        }
    }
})