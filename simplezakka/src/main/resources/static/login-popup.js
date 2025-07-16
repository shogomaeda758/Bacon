// login-popup.js の内容

document.addEventListener('DOMContentLoaded', function() {
    // 既存のログインボタンのID
    const loginBtn = document.getElementById('login-btn');

    // カスタムモーダル要素
    const loginModal = document.getElementById('loginModal');
    const closeButton = loginModal.querySelector('.close-button');
    const loginSubmitBtn = document.getElementById('loginSubmitBtn');
    const memberIdInput = document.getElementById('memberId');
    const passwordInput = document.getElementById('password');
    const errorMessage = document.getElementById('errorMessage');
    const registerBtn = document.getElementById('registerBtn');

    // --- ログインボタンをクリックでポップアップを表示 ---
    if (loginBtn) {
        loginBtn.addEventListener('click', function() {
            loginModal.style.display = 'flex'; // CSSでflexにしているので、flexにする
            errorMessage.style.display = 'none'; // エラーメッセージをリセット
            memberIdInput.value = ''; // 入力欄をクリア
            passwordInput.value = ''; // 入力欄
        });
    }

    // --- 閉じるボタンをクリックでポップアップを非表示 ---
    if (closeButton) {
        closeButton.addEventListener('click', function() {
            loginModal.style.display = 'none';
        });
    }

    // --- ポップアップの外側をクリックで非表示 ---
    // (ポップアップの背景部分をクリックした場合)
    if (loginModal) {
        window.addEventListener('click', function(event) {
            if (event.target === loginModal) { // クリックされた要素がモーダルの背景部分であるかを確認
                loginModal.style.display = 'none';
            }
        });
    }

    // --- ログインボタンのクリック処理（仮の検証ロジック） ---
    if (loginSubmitBtn) {
        loginSubmitBtn.addEventListener('click', function(event) {
            event.preventDefault(); // フォームのデフォルトの送信動作（ページのリロードなど）をキャンセル

            const memberId = memberIdInput.value;
            const password = passwordInput.value;

            // 仮の正しいログイン情報 (あくまでフロントエンドでのデモンストレーション用)
            const correctMemberId = "testuser";
            const correctPassword = "password123";

            if (memberId === correctMemberId && password === correctPassword) {
                // ログイン成功時の処理
                alert("ログインに成功しました！");
                loginModal.style.display = 'none'; // ポップアップを閉じる
                // 実際には、サーバーサイドへの認証リクエストを送り、
                // 成功したらユーザーをダッシュボードやマイページにリダイレクトします。
                // 例: window.location.href = "/dashboard.html";
            } else {
                // ログイン失敗時の処理
                errorMessage.style.display = 'block'; // エラーメッセージを表示
            }
        });
    }

    // --- 新規登録ボタンのクリック処理 ---
    if (registerBtn) {
        registerBtn.addEventListener('click', function() {
            alert("新規登録ページへ移動します"); // 例
            // 実際には、新規登録ページへリダイレクトします。
            // 例: window.location.href = "/register.html";
        });
    }
});