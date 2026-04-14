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
				icon: <Icon icon="solar:users-group-rounded-bold-duotone" size="24" />,
			},
			{
				title: "错题管理",
				path: "/management/mistake",
				icon: <Icon icon="solar:notes-bold-duotone" size="24" />,
			},
			{
				title: "标签管理",
				path: "/management/tag",
				icon: <Icon icon="solar:tag-bold-duotone" size="24" />,
			},
			{
				title: "复习记录",
				path: "/management/review-record",
				icon: <Icon icon="solar:clipboard-list-bold-duotone" size="24" />,
			},
			{
				title: "参数配置",
				path: "/management/config",
				icon: <Icon icon="solar:settings-bold-duotone" size="24" />,
			},
			{
				title: "操作日志",
				path: "/management/log",
				icon: <Icon icon="solar:document-text-bold-duotone" size="24" />,
			},
		],
	},
];
