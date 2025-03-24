package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;

public class GigaChatImageModel implements ImageModel {

    private final GigaChatClient gigaChatClient;
    private final String modelName;

    @Builder
    public GigaChatImageModel(GigaChatClient client, String modelName) {
        this.gigaChatClient = client;
        this.modelName = modelName;
    }


    @Override
    public Response<Image> generate(String s) {
        return null;
    }
}
