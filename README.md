# NZR Corretor Ortográfico

Aplicativo Android nativo para corrigir a ortografia do texto inteiro usando o corretor ortográfico instalado no próprio Android.

## Função principal

1. Escreva em WhatsApp, Discord, Instagram, navegador ou outro app compatível.
2. Selecione o texto inteiro.
3. No menu de seleção, toque em **Corrigir ortografia inteira** (em alguns aparelhos pode ficar em **Mais** / três pontos).
4. O Android envia o texto selecionado ao NZR Corretor.
5. O app aplica as sugestões do corretor pt-BR e devolve o texto corrigido ao campo original.

Também existe uma tela própria no app: cole/digite um texto e toque em **Corrigir tudo**.

## Privacidade

O projeto não pede internet e não envia o texto a servidor. A correção usa `SpellCheckerSession` / `TextServicesManager`, o mecanismo de ortografia configurado no Android.

## Observação

É necessário que o aparelho tenha um corretor ortográfico com idioma Português (Brasil) instalado/ativado. Dependendo da interface do fabricante, a opção de PROCESS_TEXT pode ficar dentro do menu de três pontos da seleção.

## Compilação

O GitHub Actions compila automaticamente o APK de teste.