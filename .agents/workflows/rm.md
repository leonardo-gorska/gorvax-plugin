---
description: executar o próximo batch do roadmap de auditoria
---

# /rm — Executar o Próximo Batch do Roadmap

## 🛡️ PROTOCOLO ZERO: BACKUP E INÍCIO

### STEP 0: Ler Estado Anterior
1. Leia o arquivo `ROADMAP.md` na raiz do projeto **inteiro**.
2. Identifique o **próximo batch pendente** — é o primeiro batch com status `[ ]` (não completado).
3. Se **todos os batches estiverem `[x]`** (concluídos), informe o usuário e **pare**. Não invente batches novos.

### STEP 1: Ler Knowledge Items Obrigatórios
// turbo
1. Leia o KI `gorvax-project-rules` para garantir conformidade com as regras do projeto.
2. Leia o KI `gorvax-architecture` para entender a estrutura de código atual.

### STEP 2: Backup
1. Crie um backup do diretório `src/` em `.husk_backups/` com nome `src_backup_<YYYY-MM-DD_HH-MM>`.
   - Comando: `Copy-Item -Path "src" -Destination ".husk_backups/src_backup_<timestamp>" -Recurse`
   - Crie o diretório `.husk_backups/` se não existir.

---

## 🚀 EXECUÇÃO DO BATCH

### STEP 3: Implementar
1. Leia a seção completa do batch pendente no `ROADMAP.md`.
2. Implemente **TODAS** as sub-tarefas do batch (BXX.1, BXX.2, etc.).
3. A interação **DEVE** ser 100% em Português (Brasil).
4. Siga rigorosamente as regras do KI `gorvax-project-rules`:
   - Código em inglês, comentários e mensagens em PT-BR.
   - Compatibilidade Bedrock obrigatória (fallback chat para AnvilGUI).
   - `ConcurrentHashMap`, save assíncrono, cache por chunks, `EnumSet`.
   - Validação de input, saldo e overlap.

### STEP 4: Atualizar `messages.yml`
1. Se o batch adicionar novos textos/mensagens, adicione as chaves correspondentes em `messages.yml`.
2. Todas as mensagens devem estar em Português (Brasil).

### STEP 5: Atualizar `MANUAL.md`
1. **Se funcionalidades novas foram criadas**: adicione documentação no `MANUAL.md`.
2. **Se funcionalidades existentes foram alteradas**: atualize a seção correspondente.
3. **Se funcionalidades foram removidas**: remova ou marque como obsoleta.
4. Atualize o "Histórico de Atualizações do Manual" ao final do arquivo.

---

## ✅ FINALIZAÇÃO

### STEP 6: Marcar Batch como Concluído
1. No `ROADMAP.md`, marque o batch executado como `[x]`.
2. Atualize a tabela de **Progresso Geral**:
   - Status do batch: `✅ Pronto`
   - `Último batch executado`: atualizar para o batch atual
   - `Próximo batch a executar`: atualizar para o próximo pendente (ou "Nenhum" se todos concluídos)
3. Adicione uma entrada na seção **📝 Log de Execução** com data, batch, executor (IA) e notas.

### STEP 7: Compilar
// turbo
1. Execute o build para verificar que o código compila sem erros:
   - `./gradlew build` (ou o comando de build do projeto)
2. Se houver erros de compilação, corrija-os antes de finalizar.

### STEP 8: Resumo
1. Informe o usuário com um resumo do que foi feito:
   - Batch executado
   - Arquivos criados/modificados
   - Funcionalidades implementadas
   - Status da compilação

---

## ⚠️ REGRAS GERAIS

- **Nunca pule batches** — execute sempre o próximo pendente em ordem.
- **Nunca crie batches novos** sem autorização do usuário.
- **Nunca modifique batches já concluídos** (`[x]`).
- **Sempre faça backup** antes de iniciar.
- **Sempre compile** ao final para garantir que nada quebrou.
- **Sempre atualize o MANUAL.md** conforme a regra obrigatória do ROADMAP.
