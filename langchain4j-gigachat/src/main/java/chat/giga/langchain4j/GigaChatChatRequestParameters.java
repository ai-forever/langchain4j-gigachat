package chat.giga.langchain4j;

import chat.giga.model.v2.completion.ToolConfigV2;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import lombok.Getter;

import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * Параметры запроса для модели GigaChat, расширяющие стандартные параметры langchain4j.
 * <p>
 * Предоставляет доступ к специфичным параметрам GigaChat API, включая параметры для потокового режима, фильтрации
 * контента, работы с функциями и другие.
 * <p>
 * Поддерживает как API v1, так и API v2 (при установке {@code useV2Completions = true}).
 *
 * @see <a href="https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/post-chat">GigaChat API
 * documentation</a>
 */
@Getter
public class GigaChatChatRequestParameters extends DefaultChatRequestParameters {

    /**
     * Параметр потокового режима ({@code "stream": "true"}). Задает минимальный интервал в секундах, который проходит
     * между отправкой токенов. Например, если указать {@code 1}, сообщения будут приходить каждую секунду, но размер каждого
     * из них будет больше, так как за секунду накапливается много токенов.
     */
    private final Integer updateInterval;

    /**
     * Указывает что сообщения надо передавать по частям в потоке. Сообщения передаются по протоколу SSE.
     */
    private final Boolean stream;

    /**
     * Включает проверку на нецензурную лексику (цензура). Выключается для аккаунтов, у которых установлен фича-флаг.
     */
    private final Boolean profanityCheck;

    /**
     * Поле, которое отвечает за то, как GigaChat будет работать с функциями (в v1 АПИ). Может быть строкой или объектом.
     * Возможные значения:
     * <ul>
     *   <li>{@code none} — режим работы по умолчанию. Если запрос не содержит function_call или значение поля — none,
     *       GigaChat не вызовет функции, а просто сгенерирует ответ в соответствии с полученными сообщениями</li>
     *   <li>{@code auto} — в зависимости от содержимого запроса, модель решает сгенерировать сообщение или вызвать функцию</li>
     *   <li>{@code {"name": "название_функции"}} — принудительная генерация аргументов для указанной функции</li>
     * </ul>
     */
    private final Object functionCall;

    /**
     * Поле, которое отвечает за то, как GigaChat будет работать с функциями (в v2 АПИ). Возможные значения: Режим
     * вызова: {@code auto}, {@code none} или {@code forced}. В режиме {@code forced} выполняется принудительный вызов
     * встроенного тула или функции из {@code tools.functions}.
     */
    private final ToolConfigV2 toolConfig;

    /**
     * Список вложений (файлов), которые будут переданы модели.
     */
    private final List<String> attachments;

    /**
     * Штраф за повторения слов. Значение {@code 1.0} — нейтральное значение. При значении больше 1 модель будет
     * стараться не повторять слова. Значение по умолчанию зависит от выбранной модели и может изменяться с обновлениями модели.
     */
    private final Float repetitionPenalty;

    /**
     * Идентификатор сессии для поддержания состояния чата.
     */
    private final String sessionId;

    /**
     * Строгая валидация JSON-схемы при использовании {@code response_format} с {@code json_schema}.
     */
    private final Boolean strictJsonSchema;

    /**
     * Использовать API v2 для запросов к модели.
     */
    private final Boolean useV2Completions;

    /**
     * Отключение фильтрации (в API v2 заменяет {@code profanity_check}).
     */
    private final Boolean disableFilter;

    /**
     * Идентификатор ассистента. Для stateful: только в первом сообщении при создании треда; для stateless — в каждом
     * запросе, где нужен ассистент. Нельзя передавать одновременно с {@code model}; при передаче id треда не передаётся.
     */
    private final String assistantId;

    /**
     * Идентификатор ячейки памяти (memory).
     */
    private final String memoryId;

    /**
     * Флаги, включающие особые возможности (например {@code preprocess}).
     */
    private final List<String> flags;

    /**
     * Степень «усилия» reasoning: {@code low}, {@code medium} или {@code high}
     */
    private final String reasoningEffort;

    private GigaChatChatRequestParameters(GigaChatBuilder builder) {
        super(builder);
        this.updateInterval = builder.updateInterval;
        this.stream = builder.stream;
        this.profanityCheck = builder.profanityCheck;
        this.functionCall = builder.functionCall;
        this.attachments = builder.attachments;
        this.repetitionPenalty = builder.repetitionPenalty;
        this.sessionId = builder.sessionId;
        this.strictJsonSchema = builder.strictJsonSchema;
        this.useV2Completions = builder.useV2Completions;
        this.disableFilter = builder.disableFilter;
        this.assistantId = builder.assistantId;
        this.memoryId = builder.memoryId;
        this.flags = builder.flags;
        this.reasoningEffort = builder.reasoningEffort;
        this.toolConfig = builder.toolConfig;
    }

    /**
     * Создает новый builder для {@link GigaChatChatRequestParameters}.
     *
     * @return новый builder
     */
    public static GigaChatBuilder builder() {
        return new GigaChatBuilder();
    }

    /**
     * Объединяет текущие параметры с другими параметрами запроса.
     * <p>
     * Создает новый объект параметров, где значения из {@code that} заменяют соответствующие
     * значения текущего объекта, если они не равны {@code null}.
     *
     * @param that другие параметры для объединения
     * @return новый объект параметров, объединяющий текущие и переданные значения
     */
    @Override
    public GigaChatChatRequestParameters overrideWith(ChatRequestParameters that) {
        return GigaChatChatRequestParameters.builder()
                .overrideWith(this)
                .overrideWith(that)
                .build();
    }

    /**
     * Builder для создания {@link GigaChatChatRequestParameters}.
     * <p>
     * Наследует стандартный builder для параметров запроса langchain4j и добавляет специфичные для GigaChat методы.
     */
    public static class GigaChatBuilder extends Builder<GigaChatBuilder> {

        private Integer updateInterval;
        private Boolean stream = false;
        private Boolean profanityCheck = false;
        private Object functionCall;
        private List<String> attachments;
        private Float repetitionPenalty;
        private String sessionId;
        private Boolean strictJsonSchema = false;
        private Boolean useV2Completions = false;
        private Boolean disableFilter = false;
        private String assistantId;
        private String memoryId;
        private List<String> flags;
        private String reasoningEffort;
        private ToolConfigV2 toolConfig;

        /**
         * Устанавливает минимальный интервал в секундах между отправкой токенов в потоковом режиме.
         *
         * @param updateInterval интервал в секундах (0 для отправки по мере генерации)
         * @return текущий builder
         */
        public GigaChatBuilder updateInterval(Integer updateInterval) {
            this.updateInterval = updateInterval;
            return this;
        }

        /**
         * Включает или выключает проверку на нецензурную лексику (цензура).
         *
         * @param profanityCheck {@code true} для включения проверки (по умолчанию {@code false})
         * @return текущий builder
         */
        public GigaChatBuilder profanityCheck(Boolean profanityCheck) {
            this.profanityCheck = profanityCheck;
            return this;
        }

        /**
         * Устанавливает режим работы с функциями.
         * <p>
         * Возможные значения:
         * <ul>
         *   <li>{@code null} или {@code "none"} — режим работы по умолчанию</li>
         *   <li>{@code "auto"} — модель решает сгенерировать сообщение или вызвать функцию</li>
         *   <li>Объект {@code {"name": "название_функции"}} — принудительная генерация аргументов для указанной функции</li>
         * </ul>
         *
         * @param functionCall режим работы с функциями
         * @return текущий builder
         */
        public GigaChatBuilder functionCall(Object functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        /**
         * Устанавливает список вложений (файлов) для передачи модели.
         *
         * @param attachments список путей или идентификаторов файлов
         * @return текущий builder
         */
        public GigaChatBuilder attachments(List<String> attachments) {
            this.attachments = attachments;
            return this;
        }

        /**
         * Включает или выключает потоковый режим ответа (SSE).
         *
         * @param stream {@code true} для включения потокового режима (по умолчанию {@code false})
         * @return текущий builder
         */
        public GigaChatBuilder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        /**
         * Устанавливает штраф за повторения слов.
         * <p>
         * Значение 1.0 — нейтральное. Значение больше 1 заставляет модель избегать повторов,
         * значение от 0 до 1 поощряет использование уже сказанных слов.
         *
         * @param repetitionPenalty коэффициент штрафа за повторения
         * @return текущий builder
         */
        public GigaChatBuilder repetitionPenalty(Float repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        /**
         * Устанавливает идентификатор сессии для поддержания состояния чата.
         *
         * @param sessionId идентификатор сессии
         * @return текущий builder
         */
        public GigaChatBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Включает или выключает строгую валидацию JSON-схемы.
         * <p>
         * Применяется только при использовании {@code response_format} с {@code json_schema}.
         *
         * @param strictJsonSchema {@code true} для включения строгой валидации (по умолчанию {@code false})
         * @return текущий builder
         */
        public GigaChatBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        /**
         * Указывает использовать API v2 для запросов к модели.
         * <p>
         * API v2 предоставляет дополнительные возможности, такие как работа с ассистентами,
         * памятью и reasoning-режимом.
         *
         * @param useV2Completions {@code true} для использования API v2 (по умолчанию {@code false})
         * @return текущий builder
         */
        public GigaChatBuilder useV2Completions(Boolean useV2Completions) {
            this.useV2Completions = useV2Completions;
            return this;
        }

        /**
         * Отключает фильтрацию контента (в API v2 заменяет {@code profanity_check}).
         *
         * @param disableFilter {@code true} для отключения фильтрации
         * @return текущий builder
         */
        public GigaChatBuilder disableFilter(Boolean disableFilter) {
            this.disableFilter = disableFilter;
            return this;
        }

        /**
         * Устанавливает идентификатор ассистента.
         * <p>
         * Для stateful-режима: только в первом сообщении при создании треда.
         * Для stateless-режима — в каждом запросе, где нужен ассистент.
         * Нельзя передавать одновременно с {@code model}.
         *
         * @param assistantId идентификатор ассистента
         * @return текущий builder
         */
        public GigaChatBuilder assistantId(String assistantId) {
            this.assistantId = assistantId;
            return this;
        }

        /**
         * Устанавливает идентификатор ячейки памяти (memory).
         *
         * @param memoryId идентификатор ячейки памяти
         * @return текущий builder
         */
        public GigaChatBuilder memoryId(String memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        /**
         * Устанавливает флаги, включающие особые возможности.
         * <p>
         * Примеры флагов: {@code preprocess} и другие.
         *
         * @param flags список флагов
         * @return текущий builder
         */
        public GigaChatBuilder flags(List<String> flags) {
            this.flags = flags;
            return this;
        }

        /**
         * Устанавливает степень усилия для reasoning-режима.
         * <p>
         * Доступные значения: {@code low}, {@code medium} или {@code high}
         * (на момент документации доступен в основном {@code medium}).
         *
         * @param reasoningEffort степень усилия reasoning
         * @return текущий builder
         */
        public GigaChatBuilder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        /**
         * Поле, которое отвечает за то, как GigaChat будет работать с функциями (в v2 АПИ).
         *
         * @param toolConfig
         * @return текущий builder
         */
        public GigaChatBuilder toolConfig(ToolConfigV2 toolConfig) {
            this.toolConfig = toolConfig;
            return this;
        }

        /**
         * Создает экземпляр {@link GigaChatChatRequestParameters} с текущими настройками builder.
         *
         * @return новый экземпляр параметров запроса
         */
        @Override
        public GigaChatChatRequestParameters build() {
            return new GigaChatChatRequestParameters(this);
        }

        /**
         * Объединяет текущий builder с параметрами из другого объекта {@link ChatRequestParameters}.
         * <p>
         * Значения из переданных параметров заменяют текущие значения builder, если они не равны {@code null}.
         *
         * @param parameters параметры для объединения
         * @return текущий builder с обновленными значениями
         */
        @Override
        public GigaChatBuilder overrideWith(ChatRequestParameters parameters) {
            super.overrideWith(parameters);
            if (parameters instanceof GigaChatChatRequestParameters chatChatRequestParameters) {
                updateInterval(getOrDefault(chatChatRequestParameters.getUpdateInterval(), updateInterval));
                profanityCheck(getOrDefault(chatChatRequestParameters.getProfanityCheck(), profanityCheck));
                functionCall(getOrDefault(chatChatRequestParameters.getFunctionCall(), functionCall));
                attachments(getOrDefault(chatChatRequestParameters.getAttachments(), attachments));
                stream(getOrDefault(chatChatRequestParameters.getStream(), stream));
                repetitionPenalty(getOrDefault(chatChatRequestParameters.getRepetitionPenalty(), repetitionPenalty));
                sessionId(getOrDefault(chatChatRequestParameters.getSessionId(), sessionId));
                strictJsonSchema(getOrDefault(chatChatRequestParameters.getStrictJsonSchema(), strictJsonSchema));
                useV2Completions(getOrDefault(chatChatRequestParameters.getUseV2Completions(), useV2Completions));
                disableFilter(getOrDefault(chatChatRequestParameters.getDisableFilter(), disableFilter));
                assistantId(getOrDefault(chatChatRequestParameters.getAssistantId(), assistantId));
                memoryId(getOrDefault(chatChatRequestParameters.getMemoryId(), memoryId));
                flags(getOrDefault(chatChatRequestParameters.getFlags(), flags));
                reasoningEffort(getOrDefault(chatChatRequestParameters.getReasoningEffort(), reasoningEffort));
                toolConfig(getOrDefault(chatChatRequestParameters.getToolConfig(), toolConfig));
            }
            return this;
        }
    }
}
