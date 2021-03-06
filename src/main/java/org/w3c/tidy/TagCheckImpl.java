/*
 *  Java HTML Tidy - JTidy
 *  HTML parser and pretty printer
 *
 *  Copyright (c) 1998-2000 World Wide Web Consortium (Massachusetts
 *  Institute of Technology, Institut National de Recherche en
 *  Informatique et en Automatique, Keio University). All Rights
 *  Reserved.
 *
 *  Contributing Author(s):
 *
 *     Dave Raggett <dsr@w3.org>
 *     Andy Quick <ac.quick@sympatico.ca> (translation to Java)
 *     Gary L Peskin <garyp@firstech.com> (Java development)
 *     Sami Lempinen <sami@lempinen.net> (release management)
 *     Fabrizio Giustina <fgiust at users.sourceforge.net>
 *
 *  The contributing author(s) would like to thank all those who
 *  helped with testing, bug fixes, and patience.  This wouldn't
 *  have been possible without all of you.
 *
 *  COPYRIGHT NOTICE:
 * 
 *  This software and documentation is provided "as is," and
 *  the copyright holders and contributing author(s) make no
 *  representations or warranties, express or implied, including
 *  but not limited to, warranties of merchantability or fitness
 *  for any particular purpose or that the use of the software or
 *  documentation will not infringe any third party patents,
 *  copyrights, trademarks or other rights. 
 *
 *  The copyright holders and contributing author(s) will not be
 *  liable for any direct, indirect, special or consequential damages
 *  arising out of any use of the software or documentation, even if
 *  advised of the possibility of such damage.
 *
 *  Permission is hereby granted to use, copy, modify, and distribute
 *  this source code, or portions hereof, documentation and executables,
 *  for any purpose, without fee, subject to the following restrictions:
 *
 *  1. The origin of this source code must not be misrepresented.
 *  2. Altered versions must be plainly marked as such and must
 *     not be misrepresented as being the original source.
 *  3. This Copyright notice may not be removed or altered from any
 *     source or altered source distribution.
 * 
 *  The copyright holders and contributing author(s) specifically
 *  permit, without fee, and encourage the use of this source code
 *  as a component for supporting the Hypertext Markup Language in
 *  commercial products. If you use this source code in a product,
 *  acknowledgment is not required but would be appreciated.
 *
 */
package org.w3c.tidy;

import static org.w3c.tidy.Versions.*;

/**
 * Check HTML attributes implementation.
 * @author Dave Raggett <a href="mailto:dsr@w3.org">dsr@w3.org </a>
 * @author Andy Quick <a href="mailto:ac.quick@sympatico.ca">ac.quick@sympatico.ca </a> (translation to Java)
 * @author Fabrizio Giustina
 * @version $Revision$ ($Author$)
 */
public final class TagCheckImpl
{

    /**
     * CheckHTML instance.
     */
    public static final TagCheck HTML = new CheckHTML();

    /**
     * CheckSCRIPT instance.
     */
    public static final TagCheck SCRIPT = new CheckSCRIPT();

    /**
     * CheckTABLE instance.
     */
    public static final TagCheck TABLE = new CheckTABLE();

    /**
     * CheckCaption instance.
     */
    public static final TagCheck CAPTION = new CheckCaption();

    /**
     * CheckIMG instance.
     */
    public static final TagCheck IMG = new CheckIMG();

    /**
     * CheckAREA instance.
     */
    public static final TagCheck AREA = new CheckAREA();

    /**
     * CheckAnchor instance.
     */
    public static final TagCheck ANCHOR = new CheckAnchor();

    /**
     * CheckMap instance.
     */
    public static final TagCheck MAP = new CheckMap();

    /**
     * CheckSTYLE instance.
     */
    public static final TagCheck STYLE = new CheckSTYLE();

    /**
     * CheckTableCell instance.
     */
    public static final TagCheck TABLECELL = new CheckTableCell();

    /**
     * CheckLINK instance.
     */
    public static final TagCheck LINK = new CheckLINK();

    /**
     * CheckHR instance.
     */
    public static final TagCheck HR = new CheckHR();

    /**
     * CheckForm instance.
     */
    public static final TagCheck FORM = new CheckForm();

    /**
     * CheckMeta instance.
     */
    public static final TagCheck META = new CheckMeta();

    /**
     * don't instantiate.
     */
    private TagCheckImpl()
    {
        // unused
    }

    /**
     * Checker implementation for html tag.
     */
    public static class CheckHTML implements TagCheck
    {
        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node) {
        	node.checkAttributes(lexer);
        }

    }

    /**
     * Checker implementation for script tags.
     */
    public static class CheckSCRIPT implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            AttVal lang, type;

            node.checkAttributes(lexer);

            lang = node.getAttrById(AttrId.LANGUAGE);
            type = node.getAttrById(AttrId.TYPE);

            if (type == null)
            {
                // check for javascript
                if (lang != null)
                {
                    final String str = lang.value;
                    if (str == null || str.regionMatches(true, 0, "javascript", 0, 10) ||
                    		str.regionMatches(true, 0, "jscript", 0, 7)) {
                        node.addAttribute("type", "text/javascript");
                    }
                    else if ("vbscript".equalsIgnoreCase(str))
                    {
                        // per Randy Waki 8/6/01
                        node.addAttribute("type", "text/vbscript");
                    }
                }
                else
                {
                    node.addAttribute("type", "text/javascript");
                }
                
                type = node.getAttrById(AttrId.TYPE);
                if (type != null) {
                    lexer.report.attrError(lexer, node, type, ErrorCode.INSERTING_ATTRIBUTE);
                } else {
                	lexer.report.missingAttr(lexer, node, "type");
                }
            }
        }

    }

    /**
     * Checker implementation for table.
     */
    public static class CheckTABLE implements TagCheck {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node) {
            final boolean hasSummary = node.getAttrById(AttrId.SUMMARY) != null;
            
            node.checkAttributes(lexer);
            
            /* a missing summary attribute is bad accessibility, no matter
               what HTML version is involved; a document without is valid */
            if (lexer.configuration.getAccessibilityCheckLevel() == 0) {
                if (!hasSummary) {
                    lexer.badAccess |= Report.MISSING_SUMMARY;
                    lexer.report.missingAttr(lexer, node, "summary");
                }
            }

            /* convert <table border> to <table border="1"> */
            if (lexer.configuration.isXmlOut()) {
            	final AttVal attval = node.getAttrByName("border");
                if (attval != null) {
                    if (attval.value == null) {
                        attval.value = "1";
                    }
                }
            }
        }
    }

    /**
     * Checker implementation for table caption.
     */
    public static class CheckCaption implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            AttVal attval;
            String value = null;

            node.checkAttributes(lexer);

            for (attval = node.attributes; attval != null; attval = attval.next)
            {
                if ("align".equalsIgnoreCase(attval.attribute))
                {
                    value = attval.value;
                    break;
                }
            }

            if (value != null)
            {
                if ("left".equalsIgnoreCase(value) || "right".equalsIgnoreCase(value))
                {
                    lexer.constrainVersion(VERS_HTML40_LOOSE);
                }
                else if ("top".equalsIgnoreCase(value) || "bottom".equalsIgnoreCase(value))
                {
                    lexer.constrainVersion(~(VERS_HTML20 | VERS_HTML32));
                }
                else
                {
                    lexer.report.attrError(lexer, node, attval, ErrorCode.BAD_ATTRIBUTE_VALUE);
                }
            }
        }

    }

    /**
     * Checker implementation for hr.
     */
    public static class CheckHR implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            final AttVal av = node.getAttrByName("src");

            node.checkAttributes(lexer);

            if (av != null)
            {
                lexer.report.attrError(lexer, node, av, ErrorCode.PROPRIETARY_ATTR_VALUE);
            }
        }
    }

    /**
     * Checker implementation for image tags.
     */
    public static class CheckIMG implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            final boolean hasAlt = node.getAttrById(AttrId.ALT) != null;
            final boolean hasSrc = node.getAttrById(AttrId.SRC) != null;
            final boolean hasUseMap = node.getAttrById(AttrId.USEMAP) != null;
            final boolean hasIsMap = node.getAttrById(AttrId.ISMAP) != null;
            final boolean hasDataFld = node.getAttrById(AttrId.DATAFLD) != null;
            
            node.checkAttributes(lexer);

            if (!hasAlt)
            {
                lexer.badAccess |= Report.MISSING_IMAGE_ALT;
                lexer.report.missingAttr(lexer, node, "alt");
                if (lexer.configuration.getAltText() != null)
                {
                    node.addAttribute("alt", lexer.configuration.getAltText());
                }
            }

            if (!hasSrc && !hasDataFld)
            {
                final AttVal missingSrc = new AttVal(null, null, '"', "src", "");
                lexer.report.attrError(lexer, node, missingSrc, ErrorCode.MISSING_ATTRIBUTE);
            }

            if (hasIsMap && !hasUseMap)
            {
                final AttVal missingIsMap = new AttVal(null, null, '"', "ismap", "");
                lexer.report.attrError(lexer, node, missingIsMap, ErrorCode.MISSING_IMAGEMAP);
            }
        }

    }

    /**
     * Checker implementation for area.
     */
    public static class CheckAREA implements TagCheck {
        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node) {
            final boolean hasAlt = node.getAttrById(AttrId.ALT) != null;
            final boolean hasHref = node.getAttrById(AttrId.HREF) != null;
            final boolean hasNoHref = node.getAttrById(AttrId.NOHREF) != null;
            
            node.checkAttributes(lexer);

            if (!hasAlt) {
            	if (lexer.configuration.getAccessibilityCheckLevel() == 0) {
            		lexer.badAccess |= Report.MISSING_LINK_ALT;
            		lexer.report.missingAttr(lexer, node, "alt");
            	}
            }
            if (!hasHref && !hasNoHref) {
            	lexer.report.missingAttr(lexer, node, "href");
            }
        }
    }

    /**
     * Checker implementation for anchors.
     */
    public static class CheckAnchor implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            node.checkAttributes(lexer);

            lexer.fixId(node);
        }
    }

    /**
     * Checker implementation for image maps.
     */
    public static class CheckMap implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            node.checkAttributes(lexer);

            lexer.fixId(node);
        }
    }

    /**
     * Checker implementation for style tags.
     */
    public static class CheckSTYLE implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            AttVal type = node.getAttrById(AttrId.TYPE);

            node.checkAttributes(lexer);

            if (type == null || type.value == null || type.value.length() == 0) {
            	type = node.repairAttrValue("type", "text/css");
                lexer.report.attrError(lexer, node, type, ErrorCode.INSERTING_ATTRIBUTE);
            }
        }
    }

    /**
     * Checker implementation for forms. Reports missing action attribute.
     */
    public static class CheckForm implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            final AttVal action = node.getAttrByName("action");

            node.checkAttributes(lexer);

            if (action == null) {
            	lexer.report.missingAttr(lexer, node, "action");
            }
        }
    }

    /**
     * Checker implementation for meta tags. Reports missing content attribute.
     */
    public static class CheckMeta implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            final AttVal content = node.getAttrByName("content");

            node.checkAttributes(lexer);

            if (content == null)
            {
                final AttVal missingAttribute = new AttVal(null, null, '"', "content", "");
                lexer.report.attrError(lexer, node, missingAttribute, ErrorCode.MISSING_ATTRIBUTE);
            }

            // name or http-equiv attribute must also be set
        }
    }

    /**
     * Checker implementation for table cells.
     */
    public static class CheckTableCell implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            node.checkAttributes(lexer);

            // HTML4 strict doesn't allow mixed content for elements with %block; as their content model

            if (node.getAttrByName("width") != null || node.getAttrByName("height") != null)
            {
                lexer.constrainVersion(~VERS_HTML40_STRICT);
            }
        }
    }

    /**
     * add missing type attribute when appropriate.
     */
    public static class CheckLINK implements TagCheck
    {

        /**
         * @see org.w3c.tidy.TagCheck#check(org.w3c.tidy.Lexer, org.w3c.tidy.Node)
         */
        public void check(final Lexer lexer, final Node node)
        {
            final AttVal rel = node.getAttrByName("rel");

            node.checkAttributes(lexer);

            if (rel != null && rel.value != null && rel.value.equals("stylesheet"))
            {
                final AttVal type = node.getAttrByName("type");

                if (type == null)
                {
                    final AttVal missingType = new AttVal(null, null, '"', "type", "");
                    lexer.report.attrError(lexer, node, missingType, ErrorCode.MISSING_ATTRIBUTE);

                    node.addAttribute("type", "text/css");
                }
            }
        }
    }

}