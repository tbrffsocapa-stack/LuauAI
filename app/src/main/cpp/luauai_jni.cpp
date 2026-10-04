#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <atomic>
#include <cstdint>

#include "llama.h"

#define LOG_TAG "LuauAI_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct LlamaState {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;
    std::atomic<bool> cancel{false};
};

static LlamaState g_state;

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * vm, void *) {
    llama_backend_init();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *, void *) {
    llama_backend_free();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_luauai_ai_engine_LlamaEngine_loadModel(
        JNIEnv * env,
        jobject,
        jstring jModelPath,
        jint nCtx,
        jint nThreads) {

    const char * path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("Carregando modelo: %s", path);

    if (g_state.ctx) {
        llama_free(g_state.ctx);
        g_state.ctx = nullptr;
    }

    if (g_state.model) {
        llama_model_free(g_state.model);
        g_state.model = nullptr;
    }

    g_state.vocab = nullptr;

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;

    g_state.model = llama_model_load_from_file(path, mparams);

    env->ReleaseStringUTFChars(jModelPath, path);

    if (!g_state.model) {
        LOGE("Falha ao carregar modelo");
        return JNI_FALSE;
    }

    g_state.vocab = llama_model_get_vocab(g_state.model);

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t)nCtx;
    cparams.n_threads = (int32_t)nThreads;
    cparams.n_threads_batch = (int32_t)nThreads;
    cparams.n_batch = 512;

    g_state.ctx = llama_init_from_model(g_state.model, cparams);

    if (!g_state.ctx) {
        LOGE("Falha ao criar contexto");
        llama_model_free(g_state.model);
        g_state.model = nullptr;
        g_state.vocab = nullptr;
        return JNI_FALSE;
    }

    LOGI("Modelo carregado com sucesso. Ctx=%d threads=%d", nCtx, nThreads);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_luauai_ai_engine_LlamaEngine_isModelLoaded(JNIEnv *, jobject) {
    return (g_state.model && g_state.ctx)
        ? JNI_TRUE
        : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_luauai_ai_engine_LlamaEngine_freeModel(JNIEnv *, jobject) {

    if (g_state.ctx) {
        llama_free(g_state.ctx);
        g_state.ctx = nullptr;
    }

    if (g_state.model) {
        llama_model_free(g_state.model);
        g_state.model = nullptr;
    }

    g_state.vocab = nullptr;

    LOGI("Modelo liberado");
}

extern "C" JNIEXPORT void JNICALL
Java_com_luauai_ai_engine_LlamaEngine_resetContext(JNIEnv *, jobject) {

    if (g_state.ctx) {
        llama_memory_clear(llama_get_memory(g_state.ctx), true);
    }

    LOGI("Contexto resetado");
}

extern "C" JNIEXPORT void JNICALL
Java_com_luauai_ai_engine_LlamaEngine_cancelGeneration(JNIEnv *, jobject) {
    g_state.cancel.store(true);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_luauai_ai_engine_LlamaEngine_generate(
        JNIEnv * env,
        jobject,
        jstring jPrompt,
        jint maxTokens,
        jfloat temperature,
        jfloat topP,
        jobject tokenCallback) {

    if (!g_state.model || !g_state.ctx || !g_state.vocab) {
        return env->NewStringUTF("[ERRO] Modelo não carregado.");
    }

    const char * prompt = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt_str(prompt);
    env->ReleaseStringUTFChars(jPrompt, prompt);

    g_state.cancel.store(false);

    std::vector<llama_token> prompt_tokens(prompt_str.size() + 32);

    int n_tokens = llama_tokenize(
        g_state.vocab,
        prompt_str.c_str(),
        (int32_t)prompt_str.size(),
        prompt_tokens.data(),
        (int32_t)prompt_tokens.size(),
        true,
        false
    );

    if (n_tokens < 0) {
        return env->NewStringUTF("[ERRO] Falha na tokenização.");
    }

    prompt_tokens.resize(n_tokens);

    jclass cbClass = nullptr;
    jmethodID cbMid = nullptr;

    if (tokenCallback) {
        cbClass = env->GetObjectClass(tokenCallback);
        cbMid = env->GetMethodID(
            cbClass,
            "onToken",
            "(Ljava/lang/String;)V"
        );
    }

    llama_memory_clear(llama_get_memory(g_state.ctx), true);

    llama_batch batch = llama_batch_get_one(
        prompt_tokens.data(),
        (int32_t)prompt_tokens.size()
    );

    if (llama_decode(g_state.ctx, batch) != 0) {
        return env->NewStringUTF("[ERRO] Falha no decode inicial.");
    }

    std::string output;
    output.reserve(2048);

    llama_token eos = llama_vocab_eos(g_state.vocab);

    auto sampler_params = llama_sampler_chain_default_params();

    llama_sampler * sampler =
        llama_sampler_chain_init(sampler_params);

    if (!sampler) {
        return env->NewStringUTF("[ERRO] Falha ao criar sampler.");
    }

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_top_k(40)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_top_p(topP, 1)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_temp(temperature)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_dist(LLAMA_DEFAULT_SEED)
    );

    const int32_t n_max = maxTokens;

    for (int32_t i = 0;
         i < n_max && !g_state.cancel.load();
         ++i) {

        llama_token token =
            llama_sampler_sample(sampler, g_state.ctx, -1);

        llama_sampler_accept(sampler, token);

        if (token == eos || llama_vocab_is_eog(g_state.vocab, token)) {
            break;
        }

        char buf[512];

        int len = llama_token_to_piece(
            g_state.vocab,
            token,
            buf,
            sizeof(buf),
            0,
            false
        );

        if (len > 0) {
            std::string piece(buf, len);
            output += piece;

            if (tokenCallback && cbMid) {
                jstring jPiece =
                    env->NewStringUTF(piece.c_str());

                env->CallVoidMethod(
                    tokenCallback,
                    cbMid,
                    jPiece
                );

                env->DeleteLocalRef(jPiece);
            }
        }

        batch = llama_batch_get_one(&token, 1);

        if (llama_decode(g_state.ctx, batch) != 0) {
            LOGE("Falha no decode do token");
            break;
        }
    }

    llama_sampler_free(sampler);

    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_luauai_ai_engine_LlamaEngine_getModelInfo(
        JNIEnv * env,
        jobject) {

    if (!g_state.model) {
        return env->NewStringUTF("Nenhum modelo carregado");
    }

    std::string info = "Modelo: ";

    char desc[256]; llama_model_desc(g_state.model, desc, sizeof(desc)); info += desc;

    info += " | Params: ";

    info += std::to_string(
        llama_model_n_params(g_state.model) / 1000000
    );

    info += "M";

    return env->NewStringUTF(info.c_str());
}
