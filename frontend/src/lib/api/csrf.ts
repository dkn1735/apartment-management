/** Header chống CSRF mà backend đòi ở mọi request ghi (spec 0004, AC-10). */
export const CSRF_HEADER = "X-Requested-With";
export const CSRF_HEADER_VALUE = "XMLHttpRequest";
