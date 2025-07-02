### 4.3. UI/UX基本方針

- **デザインの方向性**: 「丁寧な暮らし」を好む層に向けて、シンプルで余白のある画面、落ち着いた色味にする。
- **操作性**: 直感的に操作できるよう、画面構成や操作手順はシンプルにする。
- **一貫性**: ボタンの配置、ラベルの命名規則、エラーメッセージの表示形式などをシステム全体で統一する。
- **視認性**: 文字サイズや色使いに配慮し、情報が読みやすく、重要な情報が目立つようにする。
- **フィードバック**: ユーザーの操作に対して、処理中であることや処理結果（成功、エラー）を明確に表示する。
- **レスポンシブ**: スマートフォン、PCブラウザのどちらで表示しても画面レイアウトが崩れないよう配慮を行う。


 ### 4.2. 画面遷移図

以下に、主要な画面遷移を示す。

- 顧客画面
<div class="mermaid">
graph TD
    A[C01: トップページ]  --> B(C10: ログイン);
    A --> C(C02: 商品一覧);
    A --> D(C14: 特定商取引法表示)
    A --> E(C15: プライバシーポリシー)
    A --> F(C16: FAQ)

   
    
    H --> G
    B -- 既存会員 --> G(C12:マイページ)
    B -- 新規会員 --> H(C08:会員登録)
  
    

    G --> I(C13:注文履歴)
    G -- 登録情報の確認・変更 --> H

    C --> J(C03:商品詳細)
    J --> K(C04:カート)
    K -- 買い物を続ける --> C
    K -- ログアウト状態 --> P(C09:購入前ログイン)

   
    K -- ログイン状態 --> L
    L --> M(C06:注文確認)
    M --> N[C07:注文完了]

    P -- 新規登録 -->H
    H -.->L
     P -- ログイン --> L(C05:注文情報入力)

    dummy[ ] --エラー発生 --> O(C17:共通エラー画面)


    style dummy fill:#fff,stroke:#fff,stroke-width:1px
     style A fill:#e0f7fa,stroke:#0097a7
    style D fill:#e0f7fa,stroke:#0097a7
    style E fill:#e0f7fa,stroke:#0097a7
    style F fill:#e0f7fa,stroke:#

    style B fill:#e8f5e9,stroke:#2e7d32
    style G fill:#e8f5e9,stroke:#2e7d32
    style H fill:#e8f5e9,stroke:#2e7d32
    style I fill:#e8f5e9,stroke:#2e7d32

    style C fill:#fff3e0,stroke:#ef6c00
    style J fill:#fff3e0,stroke:#ef6c00

    style K fill:#ede7f6,stroke:#5e35b1
    style L fill:#ede7f6,stroke:#5e35b1
    style M fill:#ede7f6,stroke:#5e35b1
    style N fill:#ede7f6,stroke:#5e35b1
    style P fill:#ede7f6,stroke:#5e35b1
    style O fill:#d3d3d3,stroke:#808080

</div>
どの画面からでもトップページに戻れる。

- 管理者画面
<div class="mermaid">
graph TD
  A[A01:管理者ログイン] -->B(A09:管理者メニュー)
  B --> C(C02:商品管理)
  C --> D(A03:商品登録)
  C --> E(A04:商品編集)
  B --> F(A05:注文管理)
  F --> G(A06:注文詳細)
  B --> H(A08:会員管理)

 dummy[ ] --エラー発生 --> O(C17:共通エラー画面)


    style dummy fill:#fff,stroke:#fff,stroke-width:1px
      style O fill:#d3d3d3,stroke:#808080
      style A fill:#cfd8dc,stroke:#455a64
  style B fill:#b0bec5,stroke:#37474f

  style C fill:#e0f2f1,stroke:#00796b
  style D fill:#e0f2f1,stroke:#00796b
  style E fill:#e0f2f1,stroke:#00796b

  style F fill:#f3e5f5,stroke:#8e24aa
  style G fill:#f3e5f5,stroke:#8e24aa
  style H fill:#e8f5e9,stroke:#2e7d32

</div>
