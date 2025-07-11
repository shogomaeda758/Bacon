package com.example.simplezakka.exception;

// BusinessExceptionは、アプリケーションのビジネスロジックに起因する特定のエラーを示すカスタム例外です。
// RuntimeExceptionを継承することで、非検査例外となり、呼び出し元でのtry-catchブロックの記述が強制されません。
public class BusinessException extends RuntimeException {

    // この例外に紐づく独自のエラーコードを保持します。
    private final ErrorCode errorCode;

    /**
     * 指定されたエラーコードとメッセージを持つBusinessExceptionを構築します。
     * @param errorCode この例外に紐づく固有のエラーコード
     * @param message ユーザーや開発者向けの分かりやすいエラーメッセージ
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message); // 親クラス(RuntimeException)のコンストラクタを呼び出し、メッセージを設定
        this.errorCode = errorCode;
    }

    /**
     * 指定されたエラーコード、メッセージ、および原因となる例外を持つBusinessExceptionを構築します。
     * @param errorCode この例外に紐づく固有のエラーコード
     * @param message ユーザーや開発者向けの分かりやすいエラーメッセージ
     * @param cause この例外の原因となった元の例外 (e.g., SQLException, IOExceptionなど)
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause); // 親クラスのコンストラクタを呼び出し、メッセージと原因を設定
        this.errorCode = errorCode;
    }

    /**
     * このBusinessExceptionに紐づくエラーコードを取得します。
     * @return エラーコード
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}