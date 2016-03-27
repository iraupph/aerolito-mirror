package aerolito.magicmirror.module.base;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.hawk.Hawk;

import java.util.Locale;

import aerolito.magicmirror.util.L;

public abstract class Module {

    private boolean initialized;
    protected Locale locale;
    protected L logger;
    private Gson gson;

    private String formatLogMessage(String message) {
        String moduleName = getModuleIdentifier().replaceAll(".*[.]", "");
        return String.format("%s: %s", moduleName, message);
    }

    public void init(L logger, Object... args) {
        this.initialized = true;
        this.locale = new Locale("pt", "BR");
        this.logger = logger;
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
            Object storageResult = getStorageResult();
            if (storageResult != null) {
                listener.onModuleResult(storageResult);
            }
        } else {
            logger.i(formatLogMessage("Skipping storage value"), true);
        }
        new ModuleProcessAsyncTask(listener, args).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected abstract String getModuleIdentifier();

    protected TypeToken getStorageTypeToken() {
        return null;
    }

    @Nullable
    private Object getStorageResult() {
        Object stored = Hawk.get(getModuleIdentifier(), null);
        if (stored == null) {
            logger.i(formatLogMessage("No storage value"), true);
        } else {
            logger.i(formatLogMessage("Stored value notified"), true);
        }
        if (gson != null && stored != null) {
            stored = gson.fromJson(gson.toJsonTree(stored), getStorageTypeToken().getType());
        }
        return stored;
    }

    private Object putStorageResult(Object result) {
        return Hawk.put(getModuleIdentifier(), result);
    }

    @Nullable
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
            logger.i(formatLogMessage("Starting processing"), true);
            return getProcessedResult(args);
        }

        @Override
        protected void onPostExecute(Object processedResult) {
            logger.i(formatLogMessage("Finished processing"), true);
            if (processedResult != null) {
                logger.i(formatLogMessage("Processed value notified"), true);
                listener.onModuleResult(processedResult);
                putStorageResult(processedResult);
            } else {
                logger.i(formatLogMessage("No processing value"), true);
            }
        }
    }

    public interface OnModuleResult {

        void onModuleResult(Object result);
    }
}

