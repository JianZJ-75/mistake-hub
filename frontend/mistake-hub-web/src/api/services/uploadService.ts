import apiClient from "../apiClient";

const uploadImage = (file: File): Promise<string> => {

	const formData = new FormData();
	formData.append("file", file);
	return apiClient.post({
		url: "/v1/upload/image",
		data: formData,
		headers: { "Content-Type": "multipart/form-data" },
	});
};

export default { uploadImage };
