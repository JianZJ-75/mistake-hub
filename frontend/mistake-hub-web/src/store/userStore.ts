import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

import userService, { type SignInReq } from "@/api/services/userService";
import { toast } from "sonner";
import type { UserInfo, UserToken } from "#/entity";
import { StorageEnum } from "#/enum";

type UserStore = {
	userInfo: Partial<UserInfo>;
	userToken: UserToken;
	setUserInfo: (userInfo: UserInfo) => void;
	setUserToken: (token: UserToken) => void;
	clearUserInfoAndToken: () => void;
};

const useUserStore = create<UserStore>()(
	persist(
		(set) => ({
			userInfo: {},
			userToken: {},
			setUserInfo: (userInfo) => set({ userInfo }),
			setUserToken: (userToken) => set({ userToken }),
			clearUserInfoAndToken: () => set({ userInfo: {}, userToken: {} }),
		}),
		{
			name: "userStore",
			storage: createJSONStorage(() => localStorage),
			partialize: (state) => ({
				[StorageEnum.UserInfo]: state.userInfo,
				[StorageEnum.UserToken]: state.userToken,
			}),
		},
	),
);

// 每个字段/action 单独导出，避免返回对象 selector 引发无限重渲染
export const useUserInfo = () => useUserStore((state) => state.userInfo);
export const useUserToken = () => useUserStore((state) => state.userToken);
export const useUserPermissions = () => useUserStore((state) => state.userInfo.permissions || []);
export const useSetUserInfo = () => useUserStore((state) => state.setUserInfo);
export const useSetUserToken = () => useUserStore((state) => state.setUserToken);
export const useClearUserInfoAndToken = () => useUserStore((state) => state.clearUserInfoAndToken);

/** @deprecated 改用独立 hook，避免无限重渲染 */
export const useUserActions = () => useUserStore((state) => ({
	setUserInfo: state.setUserInfo,
	setUserToken: state.setUserToken,
	clearUserInfoAndToken: state.clearUserInfoAndToken,
}));

export const useSignIn = () => {
	const setUserToken = useSetUserToken();
	const setUserInfo = useSetUserInfo();

	const signIn = async (data: SignInReq) => {
		try {
			const token = await userService.loginWeb(data);
			setUserToken({ accessToken: token });
			const user = await userService.currentDetail();
			setUserInfo(user);
		} catch (err: any) {
			toast.error(err.message, { position: "top-center" });
			throw err;
		}
	};

	return signIn;
};

export default useUserStore;
