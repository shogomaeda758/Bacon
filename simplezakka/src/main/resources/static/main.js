document.addEventListener('DOMContentLoaded', function() {
    const productModal = new bootstrap.Modal(document.getElementById('productModal'));
    const cartModal = new bootstrap.Modal(document.getElementById('cartModal'));
    const orderConfirmationModal = new bootstrap.Modal(document.getElementById('orderConfirmationModal'));
    const orderCompleteModal = new bootstrap.Modal(document.getElementById('orderCompleteModal'));

    // APIのベースURL
    const API_BASE = 'http://localhost:8080/api';

    // 注文処理全体で共有するデータ構造
    // お客様情報とカート情報を一時的に保持します
    let currentOrderData = {
        customerInfo: {
            name: '',
            email: '',
            address: '',
            phoneNumber: ''
        },
        paymentMethod: '', // ★ ここに paymentMethod を移動しました
        items: [],
        totalPrice: 0 // totalPrice は最終合計額ではなく、商品合計を保持するようにします
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
                await handleError(response, '商品の取得に失敗しました');
            }
            const products = await response.json();
            displayProducts(products);
        } catch (error) {
            // handleError内でalertも行われるため、ここではログのみ
            console.error(error.message);
        }
    }

    // 商品一覧を表示する関数
    function displayProducts(products) {
        const container = document.getElementById('products-container');
        container.innerHTML = products.map(product => `
            <div class="col">
                <div class="card product-card">
                    <img src="${product.imageUrl || 'https://via.placeholder.com/300x200'}" class="card-img-top" alt="${product.name}">
                    <div class="card-body">
                        <h5 class="card-title">${product.name}</h5>
                        <p class="card-text">¥${product.price.toLocaleString()}</p>
                        <button class="btn btn-outline-primary view-product" data-id="${product.productId}">詳細を見る</button>
                    </div>
                </div>
            </div>
        `).join('');

        // イベントデリゲーションで詳細ボタンのクリックを処理
        container.querySelectorAll('.view-product').forEach(button => {
            button.addEventListener('click', function() {
                fetchProductDetail(this.dataset.id);
            });
        });
    }

    // 商品詳細を取得する関数
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

            // 在庫数と入力数量のバリデーション (suzuki_cover の修正を適用)
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

    // カートに商品を追加する関数
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

    // カート情報を取得する関数 (バッジ更新用)
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

    // カートバッジを更新する関数
    function updateCartBadge(count) {
        document.getElementById('cart-count').textContent = count;
    }

    // カートモーダルを表示する関数
    async function showCartModal() {
        await updateCartModalContent();
        toggleModal(cartModal, true);
    }

    // カートモーダルの内容を更新する関数 (カート表示と注文フォームの切り替えを内包)
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
                    const { shippingFee, grandTotal } = cart; // 分割代入で変数宣言を簡潔に

                    // suzuki_cover の foreach ループ形式を develop の map 形式に統合
                    modalBody.innerHTML = `
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
                                ${Object.values(cart.items).map(item => `
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
                                `).join('')}
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

                    // 数量更新イベントの設定
                    document.querySelectorAll('.update-quantity').forEach(input => {
                        input.addEventListener('change', function() {
                            const newQuantity = parseInt(this.value);
                            const itemId = this.dataset.id;
                            const maxStock = parseInt(this.max);

                            // バリデーションロジックは suzuki_cover を採用
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
                console.error(error.message);
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

            // 以前の入力内容を復元 (suzuki_cover の修正を適用)
            document.getElementById('name').value = currentOrderData.customerInfo.name || '';
            document.getElementById('email').value = currentOrderData.customerInfo.email || '';
            document.getElementById('address').value = currentOrderData.customerInfo.address || '';
            document.getElementById('phone').value = currentOrderData.customerInfo.phoneNumber || '';
            if (currentOrderData.paymentMethod) { // currentOrderData.paymentMethod を参照
                const radio = document.querySelector(`input[name="paymentMethod"][value="${currentOrderData.paymentMethod}"]`);
                if (radio) radio.checked = true;
            }

            // バリデーションイベントリスナーを追加 (develop の修正を適用)
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

            // 決済方法のラジオボタンに対するバリデーション表示 (suzuki_cover の修正を適用)
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
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity: parseInt(quantity) })
            });

            if (!response.ok) {
                await handleError(response, '数量の更新に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error(error.message);
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
                await handleError(response, '商品の削除に失敗しました');
            }

            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error(error.message);
        }
    }

    // お客様情報入力フォームからデータを受け取り、注文確認モーダルを表示する関数
    async function submitOrderFormAndShowConfirmation() {
        const form = document.getElementById('order-form');

        // 全体のフォームバリデーションを実行
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            // スクロールして最初の無効なフィールドにフォーカス
            form.querySelector(':invalid')?.focus(); // develop の optional chaining を適用
            return;
        }

        // 決済方法の選択チェック
        const paymentMethodElement = document.querySelector('input[name="paymentMethod"]:checked');
        const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
        if (!paymentMethodElement) {
            paymentMethodFeedback.style.display = 'block'; // エラーメッセージを表示
            alert('決済方法を選択してください。');
            return;
        } else {
            paymentMethodFeedback.style.display = 'none'; // メッセージを非表示
        }

        // 顧客情報をcurrentOrderData.customerInfoに保存
        currentOrderData.customerInfo = {
            name: document.getElementById('name').value,
            email: document.getElementById('email').value,
            address: document.getElementById('address').value,
            phoneNumber: document.getElementById('phone').value
        };

        // currentOrderData のトップレベルに paymentMethod を追加
        currentOrderData.paymentMethod = paymentMethodElement.value;

        try {
            const cartResponse = await fetch(`${API_BASE}/cart`);
            if (!cartResponse.ok) {
                const errorData = await cartResponse.json();
                throw new Error(errorData.message || 'カート情報の取得に失敗しました');
            }
            const cart = await cartResponse.json();

            // currentOrderDataにカート情報を保存
            currentOrderData.items = Object.values(cart.items);
            currentOrderData.totalPrice = cart.totalPrice; // 商品合計を保持

            toggleModal(cartModal, false); // カートモーダルを閉じる (develop の汎用関数を適用)
            showOrderConfirmation(); // 注文確認モーダルを表示

        } catch (error) {
            console.error('Error preparing order confirmation:', error);
            alert(`注文情報の準備中にエラーが発生しました: ${error.message}`);
        }
    }

    // 注文確認モーダルを表示する関数
    function showOrderConfirmation() {
        document.getElementById('orderConfirmationModalTitle').textContent = '注文内容の確認';
        const modalBody = document.getElementById('orderConfirmationModalBody');
        const modalFooter = document.getElementById('orderConfirmationModalFooter');

        // 注文商品一覧の生成
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

        // お届け先情報の生成
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

        // 支払い方法の生成 (currentOrderData.paymentMethod を参照)
        let paymentHtml = `
            <div class="confirmation-box">
                <div class="section-title">お支払い方法</div>
                <p>${currentOrderData.paymentMethod === 'bank_transfer' ? '銀行振込' :
                      currentOrderData.paymentMethod === 'cash_on_delivery' ? '代金引換' :
                      currentOrderData.paymentMethod || ''}</p>
            </div>
        `;

        modalBody.innerHTML = itemsHtml + customerHtml + paymentHtml;

        // フッターのボタン設定
        modalFooter.innerHTML = `
            <button type="button" class="btn btn-secondary" id="back-to-customer-form">戻る</button>
            <button type="button" class="btn btn-primary" id="final-confirm-order-btn">注文を確定する</button>
        `;
        
        document.getElementById('back-to-customer-form').addEventListener('click', function() {
            toggleModal(orderConfirmationModal, false); // develop の汎用関数を適用
            // カートモーダルを再表示し、注文フォームの状態にする
            toggleModal(cartModal, true); // develop の汎用関数を適用
            updateCartModalContent(true); // 注文フォームを表示
        });

        document.getElementById('final-confirm-order-btn').addEventListener('click', confirmOrder);

        toggleModal(orderConfirmationModal, true); // develop の汎用関数を適用
    }

    // 注文を確定する関数（API送信）
    async function confirmOrder() {
        try {
            // currentOrderData には既にカート情報とお客様情報が含まれている
            const response = await fetch(`${API_BASE}/order/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(currentOrderData) // currentOrderData を送信
            });

            if (!response.ok) {
                await handleError(response, '注文の確定に失敗しました');
            }

            const orderResult = await response.json();

            // カート情報をサーバーからクリア（またはAPIにクリア要求）
            await fetch(`${API_BASE}/cart`, { method: 'DELETE' });
            updateCartBadge(0); // カートバッジを0にリセット

            // フォームリセット (お客様情報入力フォームのクリア)
            const orderForm = document.getElementById('order-form');
            if (orderForm) {
                orderForm.reset();
                orderForm.classList.remove('was-validated');
                // バリデーション状態もリセット
                orderForm.querySelectorAll('.is-valid, .is-invalid').forEach(el => {
                    el.classList.remove('is-valid', 'is-invalid');
                });
                const paymentMethodFeedback = document.getElementById('paymentMethodFeedback');
                if (paymentMethodFeedback) {
                    paymentMethodFeedback.style.display = 'block'; // 初期状態に戻す
                }
            }
            // currentOrderData もリセット
            currentOrderData = {
                customerInfo: {
                    name: '', email: '', address: '', phoneNumber: ''
                },
                paymentMethod: '', // paymentMethod をトップレベルでリセット
                items: [],
                totalPrice: 0
            };

            toggleModal(orderConfirmationModal, false); // 注文確認モーダルを閉じる (develop の汎用関数を適用)
            displayOrderComplete(orderResult); // 注文完了モーダルを表示
            toggleModal(orderCompleteModal, true); // develop の汎用関数を適用


        } catch (error) {
            console.error('Error confirming order:', error);
            alert(`注文確定中にエラーが発生しました。もう一度お試しください: ${error.message}`);
        }
    }

    // 注文完了モーダルを表示する関数
    function displayOrderComplete(order) {
        document.getElementById('orderCompleteModalTitle').textContent = 'ご注文完了';
        const modalBody = document.getElementById('orderCompleteModalBody');

        const displayPaymentMethod = (order.paymentMethod === 'bank_transfer') ? '銀行振込' :
                                     (order.paymentMethod === 'cash_on_delivery') ? '代金引換' :

        modalBody.innerHTML = `
            <div class="alert alert-success" role="alert">
                <p>ご注文いただきありがとうございます。</p>
                <p>ご注文番号: <strong>${order.orderId || 'N/A'}</strong></p>
                <p>ご注文日時: ${order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A'}</p>
                <p>決済方法: ${displayPaymentMethod}</p>
            </div>
        `;
        const modalFooter = document.getElementById('orderCompleteModalFooter');
        modalFooter.innerHTML = `<button type="button" class="btn btn-primary" data-bs-dismiss="modal">閉じる</button>`;
    }

    // `index.html` にリダイレクトされた際に、カートモーダルの注文フォームを自動表示するためのロジック
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('showCart') === 'true' && urlParams.get('showCheckoutForm') === 'true') {
        toggleModal(cartModal, true);
        updateCartModalContent(true); // true を渡して注文フォームを表示
        // URLからクエリパラメータを削除して、リロード時に再度モーダルが開かないようにする
        history.replaceState({}, document.title, window.location.pathname);
    }
});