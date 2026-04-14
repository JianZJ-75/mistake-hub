import type { RouteObject } from "react-router";
import { Component } from "./utils";

export function getFrontendDashboardRoutes(): RouteObject[] {
	return [
		{ path: "workbench", element: Component("/pages/dashboard/workbench") },
		{ path: "management/user", element: Component("/pages/management/user") },
		{ path: "management/mistake", element: Component("/pages/management/mistake") },
		{ path: "management/tag", element: Component("/pages/management/tag") },
		{ path: "management/review-record", element: Component("/pages/management/review-record") },
		{ path: "management/config", element: Component("/pages/management/config") },
		{ path: "management/log", element: Component("/pages/management/log") },
	];
}
