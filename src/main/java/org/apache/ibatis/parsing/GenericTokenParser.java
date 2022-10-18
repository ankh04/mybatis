/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * 通用的占位符解析器
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken; // 占位符的开始标记
  private final String closeToken; // 占位符的结束标记
  private final TokenHandler handler; // 对应占位符的处理函数

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    // 找到占位符开始位置的索引
    int start = text.indexOf(openToken);
    if (start == -1) {
      // 没找到就直接返回
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;
    // 用来记录解析后的字符串
    final StringBuilder builder = new StringBuilder();
    // 用来记录占位符的字面值
    StringBuilder expression = null;
    do {
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue.
        // 遇到转义符，就去掉反斜杠
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // 查找到占位符的开始位置，且没有转义
        // found open token. let's search close token.
        // 往下找占位符的结束位置
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 将占位符前面的字符串添加到builder
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        // 从offset往后找closeToken
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          // 把openToken和closeToken之间的内容放到expression中
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            end = text.indexOf(closeToken, offset);
          } else {
            expression.append(src, offset, end - offset);
            break;
          }
        }
        if (end == -1) {
          // close token was not found.
          // closeToken没找到，offset拉到最后，添加全部的字符串
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // 调用handler对占位符中的内容进行处理
          // 把结果加到builder上去
          // 这里的handler是个接口，mybatis实现了好几个，有点"策略模式"的味道
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      // 让start移动到下一个openToken
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    if (offset < src.length) {
      // 如果offset还没到底，把剩下的字符串加入builder
      builder.append(src, offset, src.length - offset);
    }
    // 返回builder
    return builder.toString();
  }
}
