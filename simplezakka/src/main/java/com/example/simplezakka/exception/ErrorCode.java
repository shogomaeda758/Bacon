package com.example.simplezakka.exception;

// ErrorCodeは、アプリケーションのビジネスロジックで発生する特定の例外を識別するための列挙型です。
// 各要素は、固有のエラー状況に対応します。
public enum ErrorCode {
    // カートが空である場合に発生
    CART_EMPTY,

    // 入力データが無効である場合に発生 (e.g., 必須フィールドの欠如、フォーマット不正など)
    INVALID_INPUT,

    // 参照された商品が見つからない場合に発生
    PRODUCT_NOT_FOUND,

    // 商品の在庫が不足している場合に発生
    INSUFFICIENT_STOCK,

    // 楽観的ロック（同時更新）の失敗など、データの競合が発生した場合に発生
    OPTIMISTIC_LOCK_FAILURE,

    // 参照された顧客情報が見つからない場合に発生
    CUSTOMER_NOT_FOUND,

    // 参照された注文が見つからない場合に発生
    ORDER_NOT_FOUND
    
    // 必要に応じて、以下のようにビジネスロジック上のエラーを追加できます
    // PAYMENT_FAILED,      // 支払い処理に失敗した場合
    // UNAUTHORIZED_ACCESS, // 認証されていないアクセスの場合
    // DUPLICATE_ENTRY      // 重複するデータ登録があった場合
}