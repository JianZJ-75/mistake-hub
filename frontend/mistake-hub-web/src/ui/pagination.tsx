import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useState } from "react";

interface PaginationProps {
	current: number;
	total: number;
	pageSize: number;
	onPageChange: (page: number) => void;
	onPageSizeChange?: (size: number) => void;
	pageSizeOptions?: number[];
}

/** 生成页码按钮列表，含省略号占位 */
function getPageRange(current: number, totalPages: number): (number | "ellipsis-left" | "ellipsis-right")[] {

	if (totalPages <= 7) {
		return Array.from({ length: totalPages }, (_, i) => i + 1);
	}

	const pages: (number | "ellipsis-left" | "ellipsis-right")[] = [1];

	if (current > 4) {
		pages.push("ellipsis-left");
	}

	const start = Math.max(2, current - 2);
	const end = Math.min(totalPages - 1, current + 2);

	for (let i = start; i <= end; i++) {
		pages.push(i);
	}

	if (current < totalPages - 3) {
		pages.push("ellipsis-right");
	}

	if (!pages.includes(totalPages)) {
		pages.push(totalPages);
	}

	return pages;
}

export function Pagination({
	current,
	total,
	pageSize,
	onPageChange,
	onPageSizeChange,
	pageSizeOptions = [10, 20, 50, 100],
}: PaginationProps) {

	const [jumpValue, setJumpValue] = useState("");
	const totalPages = Math.max(1, Math.ceil(total / pageSize));
	const startItem = total === 0 ? 0 : (current - 1) * pageSize + 1;
	const endItem = Math.min(current * pageSize, total);
	const pages = getPageRange(current, totalPages);

	const handleJump = () => {
		const page = Number(jumpValue);
		if (page >= 1 && page <= totalPages && page !== current) {
			onPageChange(page);
		}
		setJumpValue("");
	};

	return (
		<div className="flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground">
			{/* 左：条目范围 */}
			<span>第 {startItem}-{endItem} 条，共 {total} 条</span>

			{/* 右：分页控件 */}
			<div className="flex items-center gap-1">
				{/* 上一页 */}
				<Button
					size="icon"
					variant="ghost"
					className="h-8 w-8"
					disabled={current <= 1}
					onClick={() => onPageChange(current - 1)}
				>
					<ChevronLeft className="h-4 w-4" />
				</Button>

				{/* 页码按钮 */}
				{pages.map((p) => {
					if (typeof p === "string") {
						return <span key={p} className="px-1 select-none">···</span>;
					}
					return (
						<Button
							key={p}
							size="sm"
							variant={p === current ? "default" : "ghost"}
							className="h-8 w-8 p-0 text-xs"
							onClick={() => p !== current && onPageChange(p)}
						>
							{p}
						</Button>
					);
				})}

				{/* 下一页 */}
				<Button
					size="icon"
					variant="ghost"
					className="h-8 w-8"
					disabled={current >= totalPages}
					onClick={() => onPageChange(current + 1)}
				>
					<ChevronRight className="h-4 w-4" />
				</Button>

				{/* 每页条数 */}
				{onPageSizeChange && (
					<Select value={String(pageSize)} onValueChange={v => onPageSizeChange(Number(v))}>
						<SelectTrigger size="sm" className="h-8 w-auto gap-1 px-2 text-xs">
							<SelectValue />
						</SelectTrigger>
						<SelectContent>
							{pageSizeOptions.map(s => (
								<SelectItem key={s} value={String(s)}>{s}条/页</SelectItem>
							))}
						</SelectContent>
					</Select>
				)}

				{/* 跳页 */}
				{totalPages > 7 && (
					<div className="flex items-center gap-1 ml-1">
						<span className="text-xs whitespace-nowrap">跳至</span>
						<Input
							className="h-8 w-12 px-1 text-center text-xs"
							value={jumpValue}
							onChange={e => setJumpValue(e.target.value.replace(/\D/g, ""))}
							onKeyDown={e => e.key === "Enter" && handleJump()}
						/>
						<span className="text-xs">页</span>
					</div>
				)}
			</div>
		</div>
	);
}
