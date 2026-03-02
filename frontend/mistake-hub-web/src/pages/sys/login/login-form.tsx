import type { SignInReq } from "@/api/services/userService";
import { useSignIn } from "@/store/userStore";
import { Button } from "@/ui/button";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/ui/form";
import { Input } from "@/ui/input";
import { cn } from "@/utils";
import { Loader2 } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { GLOBAL_CONFIG } from "@/global-config";
import { LoginStateEnum, useLoginStateContext } from "./providers/login-provider";

export function LoginForm({ className, ...props }: React.ComponentPropsWithoutRef<"form">) {
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();
	const { loginState } = useLoginStateContext();
	const signIn = useSignIn();

	const form = useForm<SignInReq>({
		defaultValues: {
			username: "admin",
			password: "",
		},
	});

	if (loginState !== LoginStateEnum.LOGIN) return null;

	const handleFinish = async (values: SignInReq) => {
		setLoading(true);
		try {
			await signIn(values);
			navigate(GLOBAL_CONFIG.defaultRoute, { replace: true });
			toast.success("登录成功", { closeButton: true });
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className={cn("flex flex-col gap-6", className)}>
			<Form {...form} {...props}>
				<form onSubmit={form.handleSubmit(handleFinish)} className="space-y-4">
					<div className="flex flex-col items-center gap-2 text-center">
						<h1 className="text-2xl font-bold">错题复习管理端</h1>
						<p className="text-balance text-sm text-muted-foreground">管理员登录</p>
					</div>

					<FormField
						control={form.control}
						name="username"
						rules={{ required: "请输入用户名" }}
						render={({ field }) => (
							<FormItem>
								<FormLabel>用户名</FormLabel>
								<FormControl>
									<Input placeholder="请输入用户名" {...field} />
								</FormControl>
								<FormMessage />
							</FormItem>
						)}
					/>

					<FormField
						control={form.control}
						name="password"
						rules={{ required: "请输入密码" }}
						render={({ field }) => (
							<FormItem>
								<FormLabel>密码</FormLabel>
								<FormControl>
									<Input type="password" placeholder="请输入密码" {...field} />
								</FormControl>
								<FormMessage />
							</FormItem>
						)}
					/>

					<Button type="submit" className="w-full" disabled={loading}>
						{loading && <Loader2 className="animate-spin mr-2" />}
						登录
					</Button>
				</form>
			</Form>
		</div>
	);
}

export default LoginForm;
