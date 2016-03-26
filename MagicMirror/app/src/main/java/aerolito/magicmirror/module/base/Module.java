package aerolito.magicmirror.module.base;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.hawk.Hawk;

import java.util.Locale;

public abstract class Module {

    private boolean initialized;
    private Gson gson;
    protected Locale locale;

    public void init(Object... args) {
        this.initialized = true;
        this.locale = new Locale("pt", "BR");
        if (getStorageTypeToken() != null) {
            this.gson = new Gson();
        }
    }

    public final void run(final OnModuleResult listener) {
        run(listener, false);
    }

    public final void run(final OnModuleResult listener, boolean skipStorage, final Object... args) {
        if (!initialized) {
            throw new IllegalStateException(String.format("\"init\" was not called for module %s", getModuleIdentifier()));
        }
        if (!skipStorage) {
            listener.onModuleResult(getStorageResult());
        }
        new ModuleProcessAsyncTask(listener, args).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected abstract String getModuleIdentifier();

    protected TypeToken getStorageTypeToken() {
        return null;
    }

    private Object getStorageResult() {
        Object stored = Hawk.get(getModuleIdentifier(), null);
        if (gson != null && stored != null) {
            stored = gson.fromJson(gson.toJsonTree(stored), getStorageTypeToken().getType());
        }
        return stored;
    }

    private Object putStorageResult(Object result) {
        return Hawk.put(getModuleIdentifier(), result);
    }

    protected abstract Object getProcessedResult(Object... args);

    private class ModuleProcessAsyncTask extends AsyncTask<Void, Void, Object> {

        private final OnModuleResult listener;
        private final Object[] args;

        public ModuleProcessAsyncTask(OnModuleResult listener, Object[] args) {
            this.listener = listener;
            this.args = args;
        }

        @Override
        protected Object doInBackground(Void... voids) {
            return getProcessedResult(args);
        }

        @Override
        protected void onPostExecute(Object processedResult) {
            listener.onModuleResult(processedResult);
            putStorageResult(processedResult);
        }
    }

    public interface OnModuleResult {

        void onModuleResult(Object result);
    }
}

