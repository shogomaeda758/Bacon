document.addEventListener('DOMContentLoaded', function() {
    // モーダル要素の取得
    const productModal = new bootstrap.Modal(document.getElementById('productModal'));
    const cartModal = new bootstrap.Modal(document.getElementById('cartModal'));
    const orderCompleteModal = new bootstrap.Modal(document.getElementById('orderCompleteModal'));
    
    // APIのベースURL
    const API_BASE = '/api';
    
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
                    <p>在庫: ${product.stock} 個</p>
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
            const quantity = parseInt(document.getElementById('quantity').value);
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
                throw new Error('カートへの追加に失敗しました');
            }
            
            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
            
            productModal.hide();
            alert('商品をカートに追加しました');
        } catch (error) {
            console.error('Error:', error);
            alert('カートへの追加に失敗しました');
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
                                            data-id="${item.id}" value="${item.quantity}" min="1" style="width: 70px">
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
                            updateItemQuantity(this.dataset.id, this.value);
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
                        <input type="text" class="form-control" id="name" required>
                        <div class="invalid-feedback">お名前を入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">メールアドレス</label>
                        <input type="email" class="form-control" id="email" required>
                        <div class="invalid-feedback">有効なメールアドレスを入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="address" class="form-label">住所</label>
                        <input type="text" class="form-control" id="address" required>
                        <div class="invalid-feedback">住所を入力してください</div>
                    </div>
                    <div class="mb-3">
                        <label for="phone" class="form-label">電話番号</label>
                        <input type="tel" class="form-control" id="phone" required>
                        <div class="invalid-feedback">電話番号を入力してください</div>
                    </div>

                    <hr class="my-4">

                    <h5>決済方法の選択</h5>
                    <div class="mb-3">
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentBankTransfer" value="bank_transfer" required>
                            <label class="form-check-label" for="paymentBankTransfer">
                                銀行振込
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="paymentMethod" id="paymentCashOnDelivery" value="cash_on_delivery" required>
                            <label class="form-check-label" for="paymentCashOnDelivery">
                                代金引換
                            </label>
                        </div>
                            </label>
                        <div class="invalid-feedback">決済方法を選択してください</div>
                    </div>
                </form>
            `;
            modalFooter.innerHTML = `
                <button type="button" class="btn btn-secondary" id="back-to-cart">カートに戻る</button>
                <button type="button" class="btn btn-primary" id="confirm-order-btn">注文を確定する</button>
            `;
            document.getElementById('back-to-cart').addEventListener('click', () => updateCartModalContent(false));
            document.getElementById('confirm-order-btn').addEventListener('click', submitOrder);
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
                throw new Error('数量の更新に失敗しました');
            }
            
            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
            alert('数量の更新に失敗しました');
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
                throw new Error('商品の削除に失敗しました');
            }
            
            const cart = await response.json();
            updateCartModalContent(); // カート表示を更新
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
            alert('商品の削除に失敗しました');
        }
    }
    
    // 注文を確定する関数
    async function submitOrder() {
        const form = document.getElementById('order-form');
        
        // フォームバリデーション
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
            return;
        }
        
        // 決済方法の選択チェック
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked');

        if (!paymentMethod) {
            alert('決済方法を選択してください。');
            return;
        }

        const orderData = {
            customerInfo: {
                name: document.getElementById('name').value,
                email: document.getElementById('email').value,
                address: document.getElementById('address').value,
                phoneNumber: document.getElementById('phone').value
            },
            // DTOに合わせて追加
            paymentMethod: paymentMethod.value
            // 配送方法のデータ送信は不要になりました
        };
        
        try {
            const response = await fetch(`${API_BASE}/order/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(orderData)
            });
            
            if (!response.ok) {
                throw new Error('注文の確定に失敗しました');
            }
            
            const order = await response.json();
            displayOrderComplete(order);
            
            cartModal.hide(); // カートモーダルを閉じる
            orderCompleteModal.show(); // 注文完了モーダルを表示
            
            // カート表示をリセット
            updateCartBadge(0);
            
            // フォームリセット
            form.reset();
            form.classList.remove('was-validated');
        } catch (error) {
            console.error('Error:', error);
            alert('注文の確定に失敗しました');
        }
    }
    
    

    // 注文完了画面を表示する関数
    function displayOrderComplete(order) {
        document.getElementById('orderCompleteBody').innerHTML = `
            <p>ご注文ありがとうございます。注文番号は <strong>${order.orderId}</strong> です。</p>
            <p>ご注文日時: ${new Date(order.orderDate).toLocaleString()}</p>
            <p>決済方法: ${order.paymentMethod === 'credit_card' ? 'クレジットカード' : order.paymentMethod === 'bank_transfer' ? '銀行振込' : '代金引換'}</p>
            <p>お客様のメールアドレスに注文確認メールをお送りしました。</p>
        `;
    }
    document.addEventListener('DOMContentLoaded', function() {
    // モーダル要素の取得
    const productModal = new bootstrap.Modal(document.getElementById('productModal'));
    const cartModal = new bootstrap.Modal(document.getElementById('cartModal'));
    const orderCompleteModal = new bootstrap.Modal(document.getElementById('orderCompleteModal'));

    // APIのベースURL
    const API_BASE = '/api';

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
            if (!response.ok) throw new Error('商品の取得に失敗しました');
            const products = await response.json();
            displayProducts(products);
        } catch (error) {
            console.error('Error:', error);
            alert('商品の読み込みに失敗しました');
        }
    }

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

            card.querySelector('.view-product').addEventListener('click', function() {
                fetchProductDetail(product.productId);
            });
        });
    }

    async function fetchProductDetail(productId) {
        try {
            const response = await fetch(`${API_BASE}/products/${productId}`);
            if (!response.ok) throw new Error('商品詳細の取得に失敗しました');
            const product = await response.json();
            displayProductDetail(product);
        } catch (error) {
            console.error('Error:', error);
            alert('商品詳細の読み込みに失敗しました');
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
                    <p>在庫: ${product.stock} 個</p>
                    <div class="d-flex align-items-center mb-3">
                        <label for="quantity" class="me-2">数量:</label>
                        <input type="number" id="quantity" class="form-control w-25" value="1" min="1" max="${product.stock}">
                    </div>
                    <button class="btn btn-primary add-to-cart" data-id="${product.productId}">カートに入れる</button>
                </div>
            </div>
        `;

        modalBody.querySelector('.add-to-cart').addEventListener('click', function() {
            const quantity = parseInt(document.getElementById('quantity').value);
            addToCart(product.productId, quantity);
        });

        productModal.show();
    }

    async function addToCart(productId, quantity) {
        try {
            const response = await fetch(`${API_BASE}/cart`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, quantity })
            });
            if (!response.ok) throw new Error('カートへの追加に失敗しました');
            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
            productModal.hide();
            alert('商品をカートに追加しました');
        } catch (error) {
            console.error('Error:', error);
            alert('カートへの追加に失敗しました');
        }
    }

    async function updateCartDisplay() {
        try {
            const response = await fetch(`${API_BASE}/cart`);
            if (!response.ok) throw new Error('カート情報の取得に失敗しました');
            const cart = await response.json();
            updateCartBadge(cart.totalQuantity);
        } catch (error) {
            console.error('Error:', error);
        }
    }

    function updateCartBadge(count) {
        document.getElementById('cart-count').textContent = count;
    }

    // 注文確認画面で「注文確定」ボタンを押したとき
    if (window.location.pathname.endsWith('order-confirm.html')) {
        const confirmBtn = document.getElementById('final-confirm-btn');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', function () {
                fetch(`${API_BASE}/cart`)
                    .then(res => res.json())
                    .then(cart => {
                        const order = {
                            orderId: Math.floor(Math.random() * 1000000),
                            orderDate: new Date().toISOString(),
                            paymentMethod: cart.payment || '未指定'
                        };
                        displayOrderComplete(order);
                        orderCompleteModal.show();
                    })
                    .catch(err => {
                        console.error('注文情報の取得に失敗しました:', err);
                        alert('注文情報の取得に失敗しました');
                    });
            });
        }
    }

    function displayOrderComplete(order) {
        document.getElementById('orderCompleteBody').innerHTML = `
            <p>ご注文ありがとうございます。注文番号は <strong>${order.orderId}</strong> です。</p>
            <p>ご注文日時: ${new Date(order.orderDate).toLocaleString()}</p>
            <p>決済方法: ${order.paymentMethod === 'credit_card' ? 'クレジットカード' : order.paymentMethod === 'bank_transfer' ? '銀行振込' : '代金引換'}</p>
            <p>お客様のメールアドレスに注文確認メールをお送りしました。</p>
        `;
    }
});

});