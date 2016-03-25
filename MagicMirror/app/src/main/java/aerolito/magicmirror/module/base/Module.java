package aerolito.magicmirror.module.base;

import android.os.AsyncTask;

import com.orhanobut.hawk.Hawk;

public abstract class Module {

    private boolean initialized;

    public void init(Object... args) {
        this.initialized = true;
    }

    public final void run(final OnModuleResult listener, final Object... args) {
        if (!initialized) {
            throw new IllegalStateException(String.format("\"init\" was not called for module %s", getModuleIdentifier()));
        }
        listener.onModuleResult(getStorageResult());
        new ModuleProcessAsyncTask(listener, args).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected abstract String getModuleIdentifier();

    private Object getStorageResult() {
        return Hawk.get(getModuleIdentifier(), null);
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

