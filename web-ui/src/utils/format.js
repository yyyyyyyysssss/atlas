import dayjs from 'dayjs';
import calendar from 'dayjs/plugin/calendar';
import isToday from 'dayjs/plugin/isToday';
import isYesterday from 'dayjs/plugin/isYesterday';

dayjs.extend(isToday)
dayjs.extend(isYesterday)

/**
 * 格式化文件大小
 * @param {number} bytes 字节数
 * @param {number} decimalPoint 保留小数位
 */
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  if (!bytes) return '-'

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  
  // 计算阶数
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  // 计算数值
  const num = bytes / Math.pow(k, i)

  // 策略：
  // 1. 如果是字节(B)，直接返回整数
  // 2. 如果数值大于 10 或者是 KB，通常取整即可
  // 3. 其他情况保留 1 位小数（如 1.2 MB 比 1 MB 更准确）
  
  const formattedNum = i <= 1 || num >= 10 
    ? Math.round(num) 
    : num.toFixed(1)

  // 去掉 1.0 这种没必要的小数点
  return parseFloat(formattedNum) + ' ' + sizes[i]
}


/**
 * 格式化时间
 * @param {string | number | Date} timeStr 后端返回的时间字符串
 */
export const formatRelativeTime = (timeStr) => {
  if (!timeStr) return ''
  
  const target = dayjs(timeStr);
  const now = dayjs()

  // 1. 如果是今天：显示 11:30
  if (target.isToday()) {
    return target.format('HH:mm')
  }

  // 2. 如果是昨天：显示 昨天
  // 如果想更精确一点可以显示 "昨天 14:22"
  if (target.isYesterday()) {
    return '昨天'
  }

  // 3. 如果是同一年（非跨年）：显示 04-02
  if (target.isSame(now, 'year')) {
    return target.format('MM-DD')
  }

  // 4. 如果是跨年：显示 2025-12-30
  return target.format('YYYY-MM-DD')
}