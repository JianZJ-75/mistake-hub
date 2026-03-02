import { Icon } from "@/components/icon";
import type { NavProps } from "@/components/nav";

export const frontendNavData: NavProps["data"] = [
	{
		name: "sys.nav.dashboard",
		items: [
			{
				title: "sys.nav.workbench",
				path: "/workbench",
				icon: <Icon icon="local:ic-workbench" size="24" />,
			},
		],
	},
	{
		name: "管理",
		items: [
			{
				title: "用户管理",
				path: "/management/user",
				icon: <Icon icon="solar:users-group-bold-duotone" size="24" />,
			},
		],
	},
];
