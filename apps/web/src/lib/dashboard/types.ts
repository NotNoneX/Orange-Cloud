// Shared types and constants for the Orange Cloud IAP dashboard.

export interface Filters {
	/** Single product scope, or null for all products. */
	productId: string | null;
	/** Trailing window in days, or null for all time. */
	days: number | null;
}

/** The dashboard only ever shows Production data; Sandbox is filtered out. */
export const ENVIRONMENT = "Production";

/** Rows per page for the paginated notification and transaction lists. */
export const PAGE_SIZE = 20;

/** Product ids present in the orange-cloud-iap database, in display order. */
export const PRODUCT_ORDER = [
	"jiamin.chen.orange_cloud.pro.lifetime",
	"jiamin.chen.orange_cloud.pro.yearly",
	"jiamin.chen.orange_cloud.pro.monthly",
] as const;

/** Short, human-friendly product labels. */
export const PRODUCT_LABEL: Record<string, string> = {
	"jiamin.chen.orange_cloud.pro.lifetime": "终身买断",
	"jiamin.chen.orange_cloud.pro.yearly": "年订阅",
	"jiamin.chen.orange_cloud.pro.monthly": "月订阅",
};

export function productLabel(id?: string | null): string {
	if (!id) return "—";
	return PRODUCT_LABEL[id] ?? id;
}

/** Human labels for App Store Server Notification V2 types. */
export const NOTIFICATION_LABEL: Record<string, string> = {
	ONE_TIME_CHARGE: "一次性购买",
	DID_RENEW: "续订成功",
	CONSUMPTION_REQUEST: "消费请求",
	SUBSCRIBED: "订阅开始",
	DID_CHANGE_RENEWAL_STATUS: "续订状态变更",
	DID_CHANGE_RENEWAL_PREF: "续订方案变更",
	DID_FAIL_TO_RENEW: "续订失败",
	EXPIRED: "订阅过期",
	GRACE_PERIOD_EXPIRED: "宽限期结束",
	REFUND: "退款",
	REFUND_DECLINED: "退款被拒",
	REFUND_REVERSED: "退款撤销",
	REVOKE: "权益撤销",
	PRICE_INCREASE: "涨价",
	RENEWAL_EXTENDED: "续订延长",
};

export function notificationLabel(type?: string | null): string {
	if (!type) return "—";
	return NOTIFICATION_LABEL[type] ?? type;
}

/** Human labels for notification subtypes (e.g. INITIAL_BUY, AUTO_RENEW_DISABLED). */
export const SUBTYPE_LABEL: Record<string, string> = {
	INITIAL_BUY: "首次购买",
	RESUBSCRIBE: "重新订阅",
	UPGRADE: "升级",
	DOWNGRADE: "降级",
	AUTO_RENEW_ENABLED: "开启自动续订",
	AUTO_RENEW_DISABLED: "关闭自动续订",
	VOLUNTARY: "自愿到期",
	BILLING_RETRY: "计费重试",
	PRICE_INCREASE: "涨价生效",
	GRACE_PERIOD: "宽限期",
	BILLING_RECOVERY: "计费恢复",
	PRODUCT_NOT_FOR_SALE: "商品已下架",
	SUMMARY: "汇总",
	FAILURE: "失败",
	ACCEPTED: "已接受",
	PENDING: "待处理",
	UNREPORTED: "未上报",
};

/** Returns a labeled subtype, or null when there is no subtype. */
export function subtypeLabel(subtype?: string | null): string | null {
	if (!subtype) return null;
	return SUBTYPE_LABEL[subtype] ?? subtype;
}

/** Human labels for transaction product types. */
export const TX_TYPE_LABEL: Record<string, string> = {
	"Non-Consumable": "买断",
	Consumable: "消耗型",
	"Auto-Renewable Subscription": "自动续订",
	"Non-Renewing Subscription": "非续订订阅",
};

export function txTypeLabel(type?: string | null): string {
	if (!type) return "—";
	return TX_TYPE_LABEL[type] ?? type;
}

/** Human labels for Apple offer types (introductory / promotional / code / win-back). */
export const OFFER_TYPE_LABEL: Record<number, string> = {
	1: "试用优惠",
	2: "促销优惠",
	3: "兑换码",
	4: "赢回优惠",
};

/** Returns a labeled offer type, or null when there is no offer. */
export function offerTypeLabel(offerType?: number | null): string | null {
	if (offerType == null) return null;
	return OFFER_TYPE_LABEL[offerType] ?? `优惠 ${offerType}`;
}

/** Apple refund reasons (App Store Server API `revocationReason`). */
export const REVOCATION_REASON_LABEL: Record<number, string> = {
	0: "其他原因（如误购）",
	1: "App 内问题",
};

export function revocationReasonLabel(reason?: number | null): string | null {
	if (reason == null) return null;
	return REVOCATION_REASON_LABEL[reason] ?? `原因 ${reason}`;
}

export type BadgeTone = "muted" | "accent" | "positive" | "negative" | "info";

/** Shared tone for a notification type, used by tables and the detail modal. */
export function notificationTone(type: string): BadgeTone {
	if (type === "REFUND_DECLINED" || type === "REFUND_REVERSED") return "info";
	if (
		type.startsWith("REFUND") ||
		type === "REVOKE" ||
		type === "EXPIRED" ||
		type === "DID_FAIL_TO_RENEW"
	) {
		return "negative";
	}
	if (type === "SUBSCRIBED" || type === "DID_RENEW" || type === "ONE_TIME_CHARGE") return "positive";
	return "info";
}

export const RANGE_OPTIONS: { value: string; days: number | null; label: string }[] = [
	{ value: "all", days: null, label: "全部" },
	{ value: "7", days: 7, label: "近 7 天" },
	{ value: "30", days: 30, label: "近 30 天" },
	{ value: "90", days: 90, label: "近 90 天" },
];

type RawSearchParams = Record<string, string | string[] | undefined>;

function getParam(sp: RawSearchParams, key: string): string | undefined {
	const v = sp[key];
	return Array.isArray(v) ? v[0] : v;
}

/** Parse URL search params into a validated Filters object. */
export function parseFilters(sp: RawSearchParams): Filters {
	const product = getParam(sp, "product");
	const productId = product && PRODUCT_LABEL[product] ? product : null;

	const daysRaw = getParam(sp, "days");
	const match = RANGE_OPTIONS.find((o) => o.value === daysRaw);
	const days = match ? match.days : null;

	return { productId, days };
}

/** Parse a 1-based page number from a search param (defaults to 1). */
export function parsePage(sp: RawSearchParams, key: string): number {
	const n = Number(getParam(sp, key));
	return Number.isInteger(n) && n > 1 ? n : 1;
}
