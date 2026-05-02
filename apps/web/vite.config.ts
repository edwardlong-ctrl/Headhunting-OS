import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || "http://localhost:8081";

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": apiProxyTarget,
      },
    },
  };
});
