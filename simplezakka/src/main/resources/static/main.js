// グローバル変数
let products = [];
let cart = [];
let categories = [];

// 画面読み込み時の処理
document.addEventListener('DOMContentLoaded', function() {
    loadCategories();
    loadProducts();
    setupEventListeners();
});

// イベントリスナーの設定
function setupEventListeners() {
    // カテゴリーボタンのクリックイベント
    document.getElementById('categoryFilter').addEventListener('click', function(e) {
        if (e.target.classList.contains('category-btn')) {
            // アクティブなボタンの状態を更新
            document.querySelectorAll('.category-btn').forEach(btn => {
                btn.classList.remove('btn-primary');
                btn.classList.add('btn-outline-primary');
            });
            e.target.classList.remove('btn-outline-primary');
            e.target.classList.add('btn-primary');
            
            // カテゴリーフィルタリング
            const selectedCategory = e.target.dataset.category;
            filterProductsByCategory(selectedCategory);
        }
    });
    
    // カートボタンのクリックイベント
    document.getElementById('cart-btn').addEventListener('click', function() {
        showCart();
    });
    
    // 検索機能
    document.getElementById('searchInput').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        searchProducts(searchTerm);
    });
}

// カテゴリー一覧の取得
function loadCategories() {
    fetch('/api/categories')
        .then(response => response.json())
        .then(data => {
            categories = data;
            updateCategoryButtons();
        })
        .catch(error => {
            console.error('カテゴリー取得エラー:', error);
        });
}

// 商品一覧の取得
function loadProducts() {
    fetch('/api/products')
        .then(response => response.json())
        .then(data => {
            products = data;
            displayProducts(products);
        })
        .catch(error => {
            console.error('商品取得エラー:', error);
        });
}

// カテゴリーボタンの更新
function updateCategoryButtons() {
    const categoryFilter = document.getElementById('categoryFilter');
    // 既存のボタンをクリア（「すべて」ボタンは残す）
    const allButton = categoryFilter.querySelector('[data-category="all"]');
    categoryFilter.innerHTML = '';
    categoryFilter.appendChild(allButton);
    
    // DBから取得したカテゴリーでボタンを動的生成
    categories.forEach(category => {
        const button = document.createElement('button');
        button.className = 'btn btn-outline-primary category-btn';
        button.dataset.category = category.categoryName;
        button.textContent = category.categoryName;
        categoryFilter.appendChild(button);
    });
}

// 商品の表示
function displayProducts(productsToShow) {
    const container = document.getElementById('products-container');
    container.innerHTML = '';
    
    productsToShow.forEach(product => {
        const productCard = createProductCard(product);
        container.appendChild(productCard);
    });
}

// 商品カードの作成
function createProductCard(product) {
    const col = document.createElement('div');
    col.className = 'col';
    
    col.innerHTML = `
        <div class="card h-100">
            <img src="${product.imageUrl}" class="card-img-top" alt="${product.name}" style="height: 200px; object-fit: cover;">
            <div class="card-body d-flex flex-column">
                <h5 class="card-title">${product.name}</h5>
                <p class="card-text text-muted small">${product.categoryName}</p>
                <p class="card-text fw-bold">¥${product.price.toLocaleString()}</p>
                <div class="mt-auto">
                    <button class="btn btn-primary me-2" onclick="showProductDetail(${product.productId})">
                        詳細を見る
                    </button>
                    <button class="btn btn-outline-primary" onclick="addToCart(${product.productId})">
                        カートに追加
                    </button>
                </div>
            </div>
        </div>
    `;
    
    return col;
}

// カテゴリー別フィルタリング
function filterProductsByCategory(category) {
    let filteredProducts;
    
    if (category === 'all') {
        filteredProducts = products;
    } else {
        filteredProducts = products.filter(product => product.categoryName === category);
    }
    
    displayProducts(filteredProducts);
}

// 商品検索
function searchProducts(searchTerm) {
    const filteredProducts = products.filter(product => 
        product.name.toLowerCase().includes(searchTerm)
    );
    displayProducts(filteredProducts);
}

// 商品詳細表示
function showProductDetail(productId) {
    fetch(`/api/products/${productId}`)
        .then(response => response.json())
        .then(product => {
            const modal = document.getElementById('productModal');
            const modalTitle = document.getElementById('productModalTitle');
            const modalBody = document.getElementById('productModalBody');
            
            modalTitle.textContent = product.name;
            modalBody.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <img src="${product.imageUrl}" class="img-fluid" alt="${product.name}">
                    </div>
                    <div class="col-md-6">
                        <h5>¥${product.price.toLocaleString()}</h5>
                        <p class="text-muted">在庫: ${product.stock}個</p>
                        <p>${product.description}</p>
                        <button class="btn btn-primary" onclick="addToCart(${product.productId})">
                            カートに追加
                        </button>
                    </div>
                </div>
            `;
            
            const bootstrapModal = new bootstrap.Modal(modal);
            bootstrapModal.show();
        })
        .catch(error => {
            console.error('商品詳細取得エラー:', error);
        });
}

// カートに追加
function addToCart(productId) {
    const product = products.find(p => p.productId === productId);
    if (!product) return;
    
    const existingItem = cart.find(item => item.productId === productId);
    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({
            productId: product.productId,
            name: product.name,
            price: product.price,
            imageUrl: product.imageUrl,
            quantity: 1
        });
    }
    
    updateCartCount();
    
    // 追加完了メッセージ
    alert(`${product.name} をカートに追加しました`);
}

// カート件数の更新
function updateCartCount() {
    const cartCount = document.getElementById('cart-count');
    const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
    cartCount.textContent = totalItems;
}

// カート表示
function showCart() {
    const modal = document.getElementById('cartModal');
    const modalBody = document.getElementById('cartModalBody');
    const modalFooter = document.getElementById('cartModalFooter');
    
    if (cart.length === 0) {
        modalBody.innerHTML = '<p>カートは空です</p>';
        modalFooter.innerHTML = '';
    } else {
        let cartHTML = '<div class="table-responsive">';
        cartHTML += '<table class="table">';
        cartHTML += '<thead><tr><th>商品</th><th>価格</th><th>数量</th><th>小計</th><th>操作</th></tr></thead>';
        cartHTML += '<tbody>';
        
        let total = 0;
        cart.forEach(item => {
            const subtotal = item.price * item.quantity;
            total += subtotal;
            
            cartHTML += `
                <tr>
                    <td>
                        <img src="${item.imageUrl}" style="width: 50px; height: 50px; object-fit: cover;" class="me-2">
                        ${item.name}
                    </td>
                    <td>¥${item.price.toLocaleString()}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateCartQuantity(${item.productId}, ${item.quantity - 1})">-</button>
                        <span class="mx-2">${item.quantity}</span>
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateCartQuantity(${item.productId}, ${item.quantity + 1})">+</button>
                    </td>
                    <td>¥${subtotal.toLocaleString()}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-danger" onclick="removeFromCart(${item.productId})">削除</button>
                    </td>
                </tr>
            `;
        });
        
        cartHTML += '</tbody>';
        cartHTML += '</table>';
        cartHTML += '</div>';
        
        modalBody.innerHTML = cartHTML;
        modalFooter.innerHTML = `
            <div class="d-flex justify-content-between w-100">
                <div class="fw-bold">合計: ¥${total.toLocaleString()}</div>
                <div>
                    <button class="btn btn-secondary" data-bs-dismiss="modal">閉じる</button>
                    <button class="btn btn-primary" onclick="proceedToOrder()">注文手続きへ</button>
                </div>
            </div>
        `;
    }
    
    const bootstrapModal = new bootstrap.Modal(modal);
    bootstrapModal.show();
}

// カート数量更新
function updateCartQuantity(productId, newQuantity) {
    if (newQuantity <= 0) {
        removeFromCart(productId);
        return;
    }
    
    const item = cart.find(item => item.productId === productId);
    if (item) {
        item.quantity = newQuantity;
        updateCartCount();
        showCart(); // カート表示を更新
    }
}

// カートから削除
function removeFromCart(productId) {
    cart = cart.filter(item => item.productId !== productId);
    updateCartCount();
    showCart(); // カート表示を更新
}

// 注文手続き
function proceedToOrder() {
    // 注文確認モーダルを表示
    showOrderConfirmation();
}

// 注文確認表示
function showOrderConfirmation() {
    const modal = document.getElementById('orderConfirmationModal');
    const modalBody = document.getElementById('orderConfirmationModalBody');
    const modalFooter = document.getElementById('orderConfirmationModalFooter');
    
    let orderHTML = '<h6>注文内容</h6>';
    orderHTML += '<div class="table-responsive">';
    orderHTML += '<table class="table table-sm">';
    
    let total = 0;
    cart.forEach(item => {
        const subtotal = item.price * item.quantity;
        total += subtotal;
        
        orderHTML += `
            <tr>
                <td>${item.name}</td>
                <td>¥${item.price.toLocaleString()}</td>
                <td>×${item.quantity}</td>
                <td>¥${subtotal.toLocaleString()}</td>
            </tr>
        `;
    });
    
    orderHTML += '</table>';
    orderHTML += '</div>';
    orderHTML += `<div class="fw-bold">合計: ¥${total.toLocaleString()}</div>`;
    
    modalBody.innerHTML = orderHTML;
    modalFooter.innerHTML = `
        <button class="btn btn-secondary" data-bs-dismiss="modal">キャンセル</button>
        <button class="btn btn-primary" onclick="completeOrder()">注文を確定する</button>
    `;
    
    // カートモーダルを閉じる
    const cartModal = bootstrap.Modal.getInstance(document.getElementById('cartModal'));
    cartModal.hide();
    
    // 注文確認モーダルを表示
    const bootstrapModal = new bootstrap.Modal(modal);
    bootstrapModal.show();
}

// 注文完了
function completeOrder() {
    // 注文完了モーダルを表示
    const modal = document.getElementById('orderCompleteModal');
    const modalBody = document.getElementById('orderCompleteModalBody');
    const modalFooter = document.getElementById('orderCompleteModalFooter');
    
    modalBody.innerHTML = `
        <div class="text-center">
            <i class="bi bi-check-circle-fill text-success" style="font-size: 3rem;"></i>
            <h5 class="mt-3">ご注文ありがとうございました</h5>
            <p>注文番号: ${generateOrderNumber()}</p>
            <p>ご指定のメールアドレスに確認メールを送信いたします。</p>
        </div>
    `;
    
    modalFooter.innerHTML = `
        <button class="btn btn-primary" onclick="clearCartAndClose()">閉じる</button>
    `;
    
    // 注文確認モーダルを閉じる
    const confirmModal = bootstrap.Modal.getInstance(document.getElementById('orderConfirmationModal'));
    confirmModal.hide();
    
    // 注文完了モーダルを表示
    const bootstrapModal = new bootstrap.Modal(modal);
    bootstrapModal.show();
}

// 注文番号生成
function generateOrderNumber() {
    const now = new Date();
    const timestamp = now.getFullYear().toString() + 
                     (now.getMonth() + 1).toString().padStart(2, '0') + 
                     now.getDate().toString().padStart(2, '0') + 
                     now.getHours().toString().padStart(2, '0') + 
                     now.getMinutes().toString().padStart(2, '0');
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return timestamp + random;
}

// カートクリアして閉じる
function clearCartAndClose() {
    cart = [];
    updateCartCount();
    
    const modal = bootstrap.Modal.getInstance(document.getElementById('orderCompleteModal'));
    modal.hide();
}