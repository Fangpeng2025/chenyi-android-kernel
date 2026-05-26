package com.chenyi.agent.tools

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * UI 树构建器 - 处理 UI 层级相关操作
 * 
 * 职责：
 * - 构建 UI 树结构
 * - 查找界面元素
 * - 转换节点为 Map
 */
object UITreeBuilder {
    
    /**
     * 构建 UI 树
     * 
     * @param node 根节点
     * @param depth 当前深度
     * @param maxDepth 最大深度
     * @return UI 树 Map
     */
    fun buildUITree(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        
        // 基本信息
        map["className"] = node.className?.toString() ?: ""
        map["text"] = node.text?.toString() ?: ""
        map["contentDescription"] = node.contentDescription?.toString() ?: ""
        map["viewIdResourceName"] = node.viewIdResourceName ?: ""
        
        // 状态
        map["isClickable"] = node.isClickable
        map["isScrollable"] = node.isScrollable
        map["isEditable"] = node.isEditable
        map["isEnabled"] = node.isEnabled
        map["isFocused"] = node.isFocused
        map["isSelected"] = node.isSelected
        map["isChecked"] = node.isChecked
        
        // 边界
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        map["bounds"] = mapOf(
            "left" to bounds.left,
            "top" to bounds.top,
            "right" to bounds.right,
            "bottom" to bounds.bottom
        )
        
        // 递归子节点
        if (depth < maxDepth && node.childCount > 0) {
            val children = mutableListOf<Map<String, Any?>>()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    children.add(buildUITree(child, depth + 1, maxDepth))
                    child.recycle()
                }
            }
            map["children"] = children
        }
        
        return map
    }
    
    /**
     * 查找元素
     * 
     * @param node 根节点
     * @param text 文本匹配
     * @param id ID 匹配
     * @param contentDesc 内容描述匹配
     * @param maxResults 最大结果数
     * @return 匹配的元素列表
     */
    fun findElements(
        node: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        contentDesc: String?,
        maxResults: Int
    ): List<Map<String, Any?>> {
        val results = mutableListOf<Map<String, Any?>>()
        findElementsRecursive(node, text, id, contentDesc, results, maxResults)
        return results
    }
    
    private fun findElementsRecursive(
        node: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        contentDesc: String?,
        results: MutableList<Map<String, Any?>>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        
        var match = true
        
        if (text != null && node.text?.toString()?.contains(text, ignoreCase = true) != true) {
            match = false
        }
        if (id != null && node.viewIdResourceName?.contains(id, ignoreCase = true) != true) {
            match = false
        }
        if (contentDesc != null && node.contentDescription?.toString()?.contains(contentDesc, ignoreCase = true) != true) {
            match = false
        }
        
        if (match) {
            results.add(nodeToMap(node))
        }
        
        // 递归子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findElementsRecursive(child, text, id, contentDesc, results, maxResults)
                child.recycle()
            }
        }
    }
    
    /**
     * 节点转 Map
     */
    fun nodeToMap(node: AccessibilityNodeInfo): Map<String, Any?> {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        return mapOf(
            "className" to (node.className?.toString() ?: ""),
            "text" to (node.text?.toString() ?: ""),
            "contentDescription" to (node.contentDescription?.toString() ?: ""),
            "viewIdResourceName" to (node.viewIdResourceName ?: ""),
            "bounds" to mapOf(
                "left" to bounds.left,
                "top" to bounds.top,
                "right" to bounds.right,
                "bottom" to bounds.bottom
            ),
            "isClickable" to node.isClickable,
            "isScrollable" to node.isScrollable,
            "isEditable" to node.isEditable,
            "isEnabled" to node.isEnabled
        )
    }
    
    /**
     * 通过文本查找可点击节点
     */
    fun findClickableNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodesRecursive(root, text, nodes)
        return nodes.firstOrNull()
    }
    
    private fun findClickableNodesRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true && node.isClickable) {
            results.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findClickableNodesRecursive(child, text, results)
            }
        }
    }
}
