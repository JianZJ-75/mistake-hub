import "./global.css";
import "./theme/theme.css";
import { addCollection } from "@iconify/react";
import solarIcons from "@iconify-json/solar/icons.json";
import ReactDOM from "react-dom/client";
import { Outlet, RouterProvider, createBrowserRouter } from "react-router";
import App from "./App";
import { registerLocalIcons } from "./components/icon";
import { GLOBAL_CONFIG } from "./global-config";
import ErrorBoundary from "./routes/components/error-boundary";
import { routesSection } from "./routes/sections";

// 注册 Solar 图标集（离线使用，不依赖 Iconify CDN）
addCollection(solarIcons as Parameters<typeof addCollection>[0]);

await registerLocalIcons();

const router = createBrowserRouter(
	[
		{
			Component: () => (
				<App>
					<Outlet />
				</App>
			),
			errorElement: <ErrorBoundary />,
			children: routesSection,
		},
	],
	{
		basename: GLOBAL_CONFIG.publicPath,
	},
);

const root = ReactDOM.createRoot(document.getElementById("root") as HTMLElement);
root.render(<RouterProvider router={router} />);
