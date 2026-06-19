<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:m="http://www.w3.org/1998/Math/MathML" xmlns:exsl="http://exslt.org/common"
                extension-element-prefixes="exsl">
    <xsl:param name="undefinedSymbol"/>
    <xsl:param name="beginMathSymbol">`</xsl:param>
    <xsl:param name="endMathSymbol">`</xsl:param>
    <xsl:variable name="open-bracket-chars" select="'( { ['"/>
    <xsl:variable name="close-bracket-chars" select="'] } )'"/>
    <xsl:variable name="amConstantsTable">
        <constants>
            <constant mml="&#8712;" ascii="in"/>
            <constant mml="&#8713;" ascii="!in"/>
            <constant mml="&#8733;" ascii="prop"/>
            <constant mml="&#8773;" ascii="~="/>
            <constant mml="&#8776;" ascii="~~"/>
            <constant mml="&#8800;" ascii="!="/>
            <constant mml="&#8801;" ascii="-="/>
            <constant mml="&#8804;" ascii="&lt;="/>
            <constant mml="&#8805;" ascii="&gt;="/>
            <constant mml="&#8826;" ascii="-&lt;"/>
            <constant mml="&#8827;" ascii="&gt;-"/>
            <constant mml="&#10927;" ascii="-&lt;="/>
            <constant mml="&#10928;" ascii="&gt;-="/>
            <constant mml="&#xb1;" ascii="+-"/>
            <constant mml="&#x2329;" ascii="(:"/>
            <constant mml="&#x232a;" ascii=":)"/>
            <constant mml="&#x222b;" ascii="int"/>
            <constant mml="&#x222e;" ascii="oint"/>
            <constant mml="&#x2202;" ascii="del"/>
            <constant mml="&#x2205;" ascii="O/"/>
            <constant mml="&#x2207;" ascii="grad"/>
            <constant mml="&#x221e;" ascii="oo"/>
            <constant mml="&#x2135;" ascii="aleph"/>
            <constant mml="&#x2234;" ascii=":."/>
            <constant mml="&#x2220;" ascii="/_"/>
            <constant mml="&#xa0;" ascii="\ "/>
            <constant mml="&#xa0;&#xa0;" ascii="quad"/>
            <constant mml="&#xa0;&#xa0;&#xa0;&#xa0;" ascii="qquad"/>
            <constant mml="&#x22ef;" ascii="cdots"/>
            <constant mml="&#x22ee;" ascii="vdots"/>
            <constant mml="&#x22f1;" ascii="ddots"/>
            <constant mml="&#x22c4;" ascii="diamond"/>
            <constant mml="&#x25a1;" ascii="square"/>
            <constant mml="&#x2308;" ascii="|~"/>
            <constant mml="&#x2309;" ascii="~|"/>
            <constant mml="&#x230a;" ascii="|__"/>
            <constant mml="&#x230b;" ascii="__|"/>
            <constant mml="&#x2102;" ascii="CC"/>
            <constant mml="&#x2115;" ascii="NN"/>
            <constant mml="&#x211a;" ascii="QQ"/>
            <constant mml="&#x211d;" ascii="RR"/>
            <constant mml="&#x2124;" ascii="ZZ"/>
            <constant mml="&#x2190;" ascii="larr"/>
            <constant mml="&#x2191;" ascii="uarr"/>
            <constant mml="&#x2192;" ascii="rarr"/>
            <constant mml="&#x2193;" ascii="darr"/>
            <constant mml="&#x2194;" ascii="harr"/>
            <constant mml="&#x21a0;" ascii="-&gt;&gt;"/>
            <constant mml="&#x21a3;" ascii="&gt;-&gt;"/>
            <constant mml="&#x21a6;" ascii="|-&gt;"/>
            <constant mml="&#x21d0;" ascii="lArr"/>
            <constant mml="&#x2916;" ascii="&gt;-&gt;&gt;"/>
            <constant mml="&#8834;" ascii="sub"/>
            <constant mml="&#8835;" ascii="sup"/>
            <constant mml="&#8838;" ascii="sube"/>
            <constant mml="&#8839;" ascii="supe"/>
            <constant mml="&#8853;" ascii="o+"/>
            <constant mml="&#8855;" ascii="ox"/>
            <constant mml="&#8857;" ascii="o."/>
            <constant mml="&#172;" ascii="not"/>
            <constant mml="&#x21d2;" ascii="=&gt;"/>
            <constant mml="&#x21d4;" ascii="iff"/>
            <constant mml="&#8866;" ascii="|--"/>
            <constant mml="&#8868;" ascii="TT"/>
            <constant mml="&#8869;" ascii="_|_"/>
            <constant mml="&#8872;" ascii="|=="/>
            <constant mml="&#8704;" ascii="AA"/>
            <constant mml="&#8707;" ascii="EE"/>
            <constant mml="&#8719;" ascii="prod"/>
            <constant mml="&#8721;" ascii="sum"/>
            <constant mml="&#8728;" ascii="@"/>
            <constant mml="&#8743;" ascii="^^"/>
            <constant mml="&#8744;" ascii="vv"/>
            <constant mml="&#8745;" ascii="nn"/>
            <constant mml="&#8746;" ascii="uu"/>
            <constant mml="&#8896;" ascii="^^^"/>
            <constant mml="&#8897;" ascii="vvv"/>
            <constant mml="&#8898;" ascii="nnn"/>
            <constant mml="&#8899;" ascii="uuu"/>
            <constant mml="&#8901;" ascii="*"/>
            <constant mml="&#x22c6;" ascii="***"/>
            <constant mml="&#x22c8;" ascii="|&gt;&lt;|"/>
            <constant mml="&#x22c9;" ascii="|&gt;&lt;"/>
            <constant mml="&#x22ca;" ascii="&gt;&lt;|"/>
            <constant mml="&#x2217;" ascii="**"/>
            <constant mml="&#215;" ascii="xx"/>
            <constant mml="\" ascii="\\"/>
            <constant mml="&#247;" ascii="-:"/>
            <constant mml="/" ascii="//"/>
            <constant mml="&#915;" ascii="Gamma"/>
            <constant mml="&#916;" ascii="Delta"/>
            <constant mml="&#920;" ascii="Theta"/>
            <constant mml="&#923;" ascii="Lambda"/>
            <constant mml="&#926;" ascii="Xi"/>
            <constant mml="&#928;" ascii="Pi"/>
            <constant mml="&#931;" ascii="Sigma"/>
            <constant mml="&#934;" ascii="Phi"/>
            <constant mml="&#936;" ascii="Psi"/>
            <constant mml="&#937;" ascii="Omega"/>
            <constant mml="&#945;" ascii="alpha"/>
            <constant mml="&#946;" ascii="beta"/>
            <constant mml="&#947;" ascii="gamma"/>
            <constant mml="&#948;" ascii="delta"/>
            <constant mml="&#949;" ascii="epsilon"/>
            <constant mml="&#950;" ascii="zeta"/>
            <constant mml="&#951;" ascii="eta"/>
            <constant mml="&#952;" ascii="theta"/>
            <constant mml="&#953;" ascii="iota"/>
            <constant mml="&#954;" ascii="kappa"/>
            <constant mml="&#955;" ascii="lambda"/>
            <constant mml="&#956;" ascii="mu"/>
            <constant mml="&#957;" ascii="nu"/>
            <constant mml="&#958;" ascii="xi"/>
            <constant mml="&#960;" ascii="pi"/>
            <constant mml="&#961;" ascii="rho"/>
            <constant mml="&#963;" ascii="sigma"/>
            <constant mml="&#964;" ascii="tau"/>
            <constant mml="&#965;" ascii="upsilon"/>
            <constant mml="&#981;" ascii="phi"/>
            <constant mml="&#967;" ascii="chi"/>
            <constant mml="&#968;" ascii="psi"/>
            <constant mml="&#969;" ascii="omega"/>
            <constant mml="&#603;" ascii="varepsilon"/>
            <constant mml="&#977;" ascii="vartheta"/>
            <constant mml="&#966;" ascii="varphi"/>
            <constant mml=" " ascii="\ "/>
        </constants>
    </xsl:variable>
    <xsl:variable name="amDefinitions" select="exsl:node-set($amConstantsTable)"/>
    <xsl:key name="am-constants-lookup" match="constant" use="@mml"/>
    <xsl:template match="constants" mode="lookup">
        <xsl:param name="mmlText"/>
        <xsl:variable name="asciiText" select="key('am-constants-lookup', $mmlText)/@ascii"/>
        <xsl:choose>
            <xsl:when test="$asciiText">
                <xsl:value-of select="$asciiText"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$mmlText"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="getStyleFunction">
        <xsl:choose>
            <xsl:when test="@mathvariant='bold'">bb</xsl:when>
            <xsl:when test="@mathvariant='script'">cc</xsl:when>
            <xsl:when test="@mathvariant='double-struck'">bbb</xsl:when>
            <xsl:when test="@mathvariant='monospace'">tt</xsl:when>
            <xsl:when test="@mathvariant='sans-serif'">sf</xsl:when>
            <xsl:when test="@mathvariant='fraktur'">fr</xsl:when>
            <xsl:when test="@mathvariant='bold-script'">bb cc</xsl:when>
            <xsl:when test="@mathvariant='bold-sans-serif'">bb sf</xsl:when>
            <xsl:when test="@mathvariant='bold-fraktur'">bb fr</xsl:when>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="/">
        <xsl:apply-templates select="node()" mode="notMath"/>
    </xsl:template>
    <xsl:template match="node()|@*" mode="notMath">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" mode="notMath"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="text()">
        <xsl:if test="parent::m:mo|parent::m:mi|parent::m:mn|parent::m:ms|parent::m:mtext">
            <xsl:choose>
                <xsl:when test=". = ' '">
                    <xsl:apply-templates select="$amDefinitions/constants" mode="lookup">
                        <xsl:with-param name="mmlText" select="' '"/>
                    </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="valueStr" select="normalize-space(.)"/>
                    <xsl:apply-templates select="$amDefinitions/constants" mode="lookup">
                        <xsl:with-param name="mmlText" select="$valueStr"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    <xsl:template match="*" name="undefined">
        <xsl:choose>
            <xsl:when test="self::m:*">
                <xsl:text>mml&lt;</xsl:text>
                <xsl:value-of select="local-name()"/>
                <xsl:for-each select="@*">
                    <xsl:text></xsl:text>
                    <xsl:value-of select="name()"/>
                    <xsl:text>="</xsl:text>
                    <xsl:value-of select="."/>
                    <xsl:text>"</xsl:text>
                </xsl:for-each>
                <xsl:text>/&gt;</xsl:text>
                <xsl:call-template name="functionArgElement">
                    <xsl:with-param name="argElements" select="node()"/>
                    <xsl:with-param name="useBrackets" select="true()"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$undefinedSymbol"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="spacedGroup">
        <xsl:param name="groupContent" select="*"/>
        <xsl:variable name="result">
            <xsl:for-each select="$groupContent">
                <xsl:variable name="child_asciimath">
                    <xsl:apply-templates select="."/>
                </xsl:variable>
                <xsl:if test="$child_asciimath != ''">
                    <xsl:text></xsl:text>
                    <xsl:value-of select="$child_asciimath"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>
        <xsl:if test="$result != ''">
            <xsl:value-of select="$result"/>
        </xsl:if>
    </xsl:template>
    <xsl:template name="functionArgElement">
        <xsl:param name="argElements" select="*"/>
        <xsl:param name="useBrackets" select="true()"/>
        <xsl:choose>
            <xsl:when test="count($argElements) = 1">
                <xsl:apply-templates select="$argElements">
                    <xsl:with-param name="functionArg" select="$useBrackets"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$useBrackets">
                    <xsl:text>(</xsl:text>
                </xsl:if>
                <xsl:call-template name="spacedGroup">
                    <xsl:with-param name="groupContent" select="$argElements"/>
                </xsl:call-template>
                <xsl:if test="$useBrackets">
                    <xsl:text>)</xsl:text>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="unaryFunction">
        <xsl:param name="functionName" select="local-name()"/>
        <xsl:param name="argElements" select="*"/>
        <xsl:value-of select="$functionName"/>
        <xsl:if test="count($argElements) = 1 and count(m:mrow) = 0">
            <xsl:text></xsl:text>
        </xsl:if>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="$argElements"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="binaryFunction">
        <xsl:param name="functionName" select="local-name"/>
        <xsl:param name="arg1Elements" select="*[1]"/>
        <xsl:param name="arg2Elements" select="*[2]"/>
        <xsl:value-of select="$functionName"/>
        <xsl:if test="count($arg1Elements) = 1 and count($arg1Elements[self::m:mrow]) = 0">
            <xsl:text></xsl:text>
        </xsl:if>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="$arg1Elements"/>
        </xsl:call-template>
        <xsl:if test="(count($arg1Elements) = 1 and count($arg1Elements[self::m:mrow]) = 0) or (count($arg2Elements) = 1 and count($arg2Elements[self::m:mrow]) = 0)">
            <xsl:text></xsl:text>
        </xsl:if>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="$arg2Elements"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="infixFunction">
        <xsl:param name="functionName" select="local-name()"/>
        <xsl:param name="arg1Elements" select="*[1]"/>
        <xsl:param name="arg2Elements" select="*[2]"/>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="$arg1Elements"/>
            <xsl:with-param name="useBrackets" select="not($functionName = '^' or $functionName = '_')"/>
        </xsl:call-template>
        <xsl:value-of select="$functionName"/>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="$arg2Elements"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:mrow" name="mrow">
        <xsl:param name="functionArg" select="false()"/>
        <xsl:variable name="isMrow" select="self::m:mrow"/>
        <xsl:choose>
            <!-- Handle spaced elements -->
            <xsl:when
                    test="$isMrow and child::*[position() = 1 and self::m:mspace and @width='1ex'] and child::*[position() = 3 and self::m:mspace and @width='1ex'] and child::*[position() = 2 and self::m:mo and . = 'if']">
                <xsl:apply-templates select="*[2]"/>
            </xsl:when>
            <xsl:when
                    test="$isMrow and count(child::*) = 3 and child::*[position() = 1 and self::m:mo and contains($open-bracket-chars, text())] and child::*[position() = last() and self::m:mo and contains($close-bracket-chars, text())] and child::*[position() = 2 and self::m:mtable]">
                <xsl:call-template name="matrix">
                    <xsl:with-param name="openBracket" select="*[1]"/>
                    <xsl:with-param name="tableElement" select="*[2]"/>
                    <xsl:with-param name="closeBracket" select="*[3]"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$functionArg">
                    <xsl:text>(</xsl:text>
                </xsl:if>
                <xsl:call-template name="spacedGroup"/>
                <xsl:if test="$functionArg">
                    <xsl:text>)</xsl:text>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:math" mode="notMath">
        <xsl:value-of select="$beginMathSymbol"/>
        <xsl:call-template name="spacedGroup"/>
        <xsl:value-of select="$endMathSymbol"/>
    </xsl:template>
    <xsl:template match="m:mn|m:mi|m:mo">
        <xsl:choose>
            <xsl:when test="self::m:mo[@linebreak='newline'] and count(*) = 0">
                <xsl:text>newline</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="tokens"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="tokens">
        <!--  We can rely on tokens not needing brackets for functions for handling mathvariant -->
        <xsl:variable name="styleFunction">
            <xsl:call-template name="getStyleFunction"/>
        </xsl:variable>
        <xsl:variable name="styleFunctionNormalized" select="normalize-space($styleFunction)"/>
        <xsl:if test="$styleFunctionNormalized">
            <xsl:value-of select="$styleFunctionNormalized"/>
            <xsl:text></xsl:text>
        </xsl:if>
        <xsl:apply-templates select="node()"/>
    </xsl:template>
    <xsl:template match="m:mfrac">
        <xsl:param name="functionArg" select="false()"/>
        <xsl:choose>
            <xsl:when test="@bevelled='true'">
                <xsl:call-template name="binaryFunction">
                    <xsl:with-param name="functionName">bfrac</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$functionArg">
                <xsl:call-template name="binaryFunction">
                    <xsl:with-param name="functionName">frac</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="infixFunction">
                    <xsl:with-param name="functionName">/</xsl:with-param>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:msup">
        <xsl:call-template name="infixFunction">
            <xsl:with-param name="functionName">^</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:msub">
        <xsl:call-template name="infixFunction">
            <xsl:with-param name="functionName">_</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:msubsup|m:munderover">
        <xsl:apply-templates select="*[1]"/>
        <xsl:text>_</xsl:text>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="*[2]"/>
        </xsl:call-template>
        <xsl:text>^</xsl:text>
        <xsl:call-template name="functionArgElement">
            <xsl:with-param name="argElements" select="*[3]"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:msqrt" name="sqrt">
        <xsl:call-template name="unaryFunction">
            <xsl:with-param name="functionName">sqrt</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:mroot">
        <xsl:call-template name="binaryFunction">
            <xsl:with-param name="functionName">root</xsl:with-param>
            <xsl:with-param name="arg1Elements" select="*[2]"/>
            <xsl:with-param name="arg2Elements" select="*[1]"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="m:mstyle[@mathvariant]">
        <xsl:variable name="styleFunction">
            <xsl:call-template name="getStyleFunction"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$styleFunction">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="functionName" select="normalize-space($styleFunction)"/>
                </xsl:call-template>
            </xsl:when>
            <!-- 			<xsl:when test="@mathvariant='fraktur'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">fr</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <!-- 			<xsl:when test="@mathvariant='bold'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">bb</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <!-- 			<xsl:when test="@mathvariant='script'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">cc</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <!-- 			<xsl:when test="@mathvariant='double-struck'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">bbb</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <!-- 			<xsl:when test="@mathvariant='monospace'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">tt</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <!-- 			<xsl:when test="@mathvariant='sans-serif'"> -->
            <!-- 				<xsl:call-template name="unaryFunction"> -->
            <!-- 					<xsl:with-param name="functionName">sf</xsl:with-param> -->
            <!-- 				</xsl:call-template> -->
            <!-- 			</xsl:when> -->
            <xsl:otherwise>
                <xsl:call-template name="undefined"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:mover">
        <xsl:variable name="operator" select="normalize-space(*[position() = 2 and self::m:mo]/text())"/>
        <xsl:choose>
            <xsl:when test="$operator = '&#x5e;'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">hat</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$operator = '&#xaf;'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">bar</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$operator = '&#x2192;'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">vec</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$operator = '.'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">dot</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$operator = '..'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">ddot</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="infixFunction">
                    <xsl:with-param name="functionName">_^</xsl:with-param>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:munder">
        <xsl:variable name="operator" select="*[position() = 2 and self::m:mo]"/>
        <xsl:variable name="funcName" select="*[position() = 1 and self::m:mo]"/>
        <xsl:choose>
            <xsl:when test="contains(' lim &#x2211; &#x220f; ', concat(' ', $funcName, ' '))">
                <xsl:call-template name="infixFunction">
                    <xsl:with-param name="functionName">_</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$operator = '&#x332;'">
                <xsl:call-template name="unaryFunction">
                    <xsl:with-param name="argElements" select="*[1]"/>
                    <xsl:with-param name="functionName">ul</xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="infixFunction">
                    <xsl:with-param name="functionName">__</xsl:with-param>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:mfenced">
        <xsl:choose>
            <xsl:when test="@open">
                <xsl:value-of select="@open"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>(</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:for-each select="*">
            <xsl:apply-templates select="."/>
            <xsl:if test="position() &lt; last()">
                <xsl:choose>
                    <xsl:when test="../@separators">
                        <xsl:choose>
                            <xsl:when test="string-length(../@separators) &gt; position()">
                                <xsl:value-of select="substring(../@separators, position(), 1)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="substring(../@separators, string-length(../@separators), 1)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>,</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text></xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:choose>
            <xsl:when test="@close">
                <xsl:value-of select="@close"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>)</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:mtext">
        <xsl:variable name="styleFunction">
            <xsl:call-template name="getStyleFunction"/>
        </xsl:variable>
        <xsl:variable name="styleFunctionNormalized" select="normalize-space($styleFunction)"/>
        <xsl:if test="$styleFunctionNormalized">
            <xsl:value-of select="$styleFunctionNormalized"/>
            <xsl:text></xsl:text>
        </xsl:if>
        <xsl:text>text(</xsl:text>
        <xsl:apply-templates select="node()"/>
        <xsl:text>)</xsl:text>
    </xsl:template>
    <xsl:template match="m:semantics">
        <xsl:variable name="presentationMathML"
                      select="m:annotation-xml[@encoding='MathML-Presentation' or @encoding='application/mathml-presentation'][1]"/>
        <xsl:choose>
            <xsl:when test="$presentationMathML">
                <xsl:apply-templates select="$presentationMathML"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="*[1]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="m:annotation-xml">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    <xsl:template match="m:menclose">
        <xsl:param name="functionArg" select="false()"/>
        <xsl:choose>
            <xsl:when test="contains(@notation, 'radical')">
                <xsl:call-template name="sqrt"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="mrow">
                    <xsl:with-param name="functionArg" select="$functionArg"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- Currently just ignore the mphantom, ideally we would space according to the space the content would occupy. -->
    <xsl:template match="m:mphantom">
    </xsl:template>
    <!--  Probably should be more advanced handling to allow for different sized spaces. -->
    <xsl:template match="m:mspace">
        <xsl:choose>
            <xsl:when test="@linebreak='newline'">
                <xsl:text>newline</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>\</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="matrix">
        <xsl:param name="openBracket" select="'('"/>
        <xsl:param name="closeBracket" select="')'"/>
        <xsl:param name="tableElement" select="."/>
        <xsl:value-of select="$openBracket"/>
        <xsl:for-each select="$tableElement/m:mtr">
            <xsl:value-of select="$openBracket"/>
            <xsl:for-each select="m:mtd">
                <xsl:apply-templates select="*"/>
                <xsl:if test="position() != last()">
                    <xsl:text>,</xsl:text>
                </xsl:if>
            </xsl:for-each>
            <xsl:value-of select="$closeBracket"/>
            <xsl:if test="position() != last()">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:value-of select="$closeBracket"/>
    </xsl:template>
</xsl:stylesheet>