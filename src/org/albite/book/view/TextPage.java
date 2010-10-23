package org.albite.book.view;

import org.albite.book.model.parser.TextParser;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import org.albite.albite.ColorScheme;
import org.albite.font.AlbiteFont;
import org.albite.util.archive.zip.ArchiveZip;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenationInfo;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextTeXHyphenator;

public class TextPage
        extends Page
        implements StylingConstants {

    private int                 start;

    /*
     * start+length, i.e. character is in page if start <= char_pos < end
     */
    private int                 end;

    protected Vector            regions;
    private ImageRegion         imageRegion = null;
 
    public TextPage(final Booklet booklet, final PageState ip) {
        this.booklet = booklet;

        final int width = booklet.width;
        final int height = booklet.height;

        // App Settings
        final byte defaultAlign = booklet.defaultAlign;
        final AlbiteFont fontPlain = booklet.fontPlain;
        final AlbiteFont fontItalic = booklet.fontItalic;
        final int spaceWidth = fontPlain.spaceWidth;
              int dashWidth  = 0;
        final int fontHeight = booklet.fontHeight;
        final int fontHeightX2 = 2 * fontHeight;
        final int fontIndent = booklet.fontIndent;
        final ZLTextTeXHyphenator hyphenator = booklet.hyphenator;

        // Chapter settings
        final char[] buffer = booklet.getTextBuffer();
        final int bufferSize;
        final ArchiveZip bookFile = booklet.bookArchive;
        final Vector images = ip.images;

        byte style;
        boolean center;
        byte color;

        AlbiteFont font;

        HyphenatedTextRegion lastHyphenatedWord;
        boolean startsNewParagraph;

        TextParser parser = ip.parser;
        int wordPixelWidth; //word width in pixels

        Vector wordsOnThisLine = new Vector(20); //RegionTexts

        boolean firstWord;

        int posX = 0;
        int posY = 0;

        if (images.isEmpty()) {
            //text mode
            regions = new Vector(300);

            parser.position = end = start = ip.position;
            parser.length = ip.length;

            bufferSize = buffer.length;

            style = ip.style;
            center = ip.center;

            lastHyphenatedWord = ip.lastHyphenatedWord;
            startsNewParagraph = ip.startsNewParagraph;

        } else {
            //image mode
            ImageRegion ri = (ImageRegion) images.firstElement();
            images.removeElementAt(0);

            imageRegion = ri;
            regions = new Vector(40);

            posY = 0;

            bufferSize = ri.altTextBufferPosition + ri.altTextBufferLength;
            parser.position = end = start = ri.altTextBufferPosition;
            parser.length = 0;

            style = ITALIC;
            center = true;

            lastHyphenatedWord = null;
            startsNewParagraph = true;
        }

        /*
         * Setup font & color, based on style value from previous page.
         */
        font = chooseFont(fontPlain, fontItalic, style);
        color = chooseTextColor(style);

        boolean lastLine = false;
        boolean firstLine = true;
        boolean lineBreak = false;

        page:
            while (true) {

                /*
                 * There is no more space for new lines,
                 * so the page is done.
                 */
                if (posY >= height - fontHeight) {
                    break;
                }

                /*
                 * Check if it is the last line of the page
                 */
                if (posY >= height - (fontHeightX2)) {
                    lastLine = true;
                }

                /*
                 * NB: posX & posY are in pixels, pos is in chars.
                 */
                posX = 0;
                firstWord = true;

                /*
                 * Clear the cache that will hold all the elements on the line
                 */
                wordsOnThisLine.removeAllElements();

                /*
                 * Indent the line, if it starts a new paragraph.
                 */
                if (startsNewParagraph) {
                    posX = fontIndent;
                }

                line:
                    while (true) {

                        /*
                         * Parse on
                         */
                        if (!parser.parseNext(buffer, bufferSize)) {

                            /* No more chars to read */

                            if (imageRegion == null) {
                                ip.bufferRead = true;
                            }

                            lineBreak = true;

                            if (wordsOnThisLine.size() > 0) {
                                positionWordsOnLine(wordsOnThisLine, width,
                                        posY, spaceWidth, fontIndent, lineBreak,
                                        startsNewParagraph, center);

                            }

                            break page;
                        }


                        /*
                         * Logic for possible parsing states.
                         */
                        final int state = parser.state;
                        switch (state) {
                            case TextParser.STATE_PASS:
                                continue line;

                            case TextParser.STATE_NEW_SOFT_LINE:
                                if (posX == 0) {
                                    /*
                                     * Only if it's on the next line
                                     */
                                    startsNewParagraph = true;
                                }

                                if (!(posX > (startsNewParagraph ? fontIndent : 0))) {
                                    continue line;
                                }

                            case TextParser.STATE_NEW_LINE: //linebreak
                                if (!firstLine || (posX >
                                        (startsNewParagraph ? fontIndent : 0)
                                        )) {
                                    lineBreak = true;
                                    break line;
                                } else {
                                    /* don't start a page with blank lines */
                                    continue line;
                                }

                            case TextParser.STATE_STYLING:

                                /* enable styling */
                                if (parser.enableBold) {
                                    style |= BOLD;
                                }

                                if (parser.enableItalic) {
                                    style |= ITALIC;
                                }

                                if (parser.enableHeading) {
                                    style |= HEADING;
                                }

                                if (parser.enableCenterAlign) {
                                    center = true;
                                }

                                if (parser.disableCenterAlign) {
                                    center = false;
                                }

                                /* disable styling */
                                if (parser.disableBold) {
                                    style &= ~BOLD;
                                }

                                if (parser.disableItalic) {
                                    style &= ~ITALIC;
                                }

                                if (parser.disableHeading) {
                                    style &= ~HEADING;
                                }

                                /* setup font & color */
                                font = chooseFont(fontPlain,
                                        fontItalic, style);
                                color = chooseTextColor(style);
                                continue line;

                            case TextParser.STATE_IMAGE:

                                if (booklet.renderImages) {
                                    ImageRegion ri = new ImageRegion(
                                            (bookFile == null
                                                ? null
                                                : bookFile.getEntry(
                                                    new String(buffer,
                                                        parser.imageURLPosition,
                                                        parser.imageURLLength))
                                                    ),
                                            parser.imageTextPosition,
                                            parser.imageTextLength);
                                    images.addElement(ri);
                                }

                                continue line;

                            case TextParser.STATE_RULER:

                                regions.addElement(
                                        new RulerRegion(
                                        (short) 0,
                                        (short) posY,
                                        (short) width,
                                        (short) font.lineHeight,
                                        ColorScheme.COLOR_TEXT));
                                break line;

                            default:
                                /*
                                 * There is nothing to do. It must be
                                 * STATE_NORMAL
                                 */
                        }

                        if (parser.length == 0) {
                            continue line;
                        }

                        wordPixelWidth = font.charsWidth(buffer,
                                parser.position, parser.length);

                        if (!firstWord) {
                            /*
                             * If it is not the first word, it will need the
                             * space(s) before it
                             */
                            posX += font.spaceWidth;
                        }

                        /*
                         * word FITS on the line without need to split it
                         */
                        if (wordPixelWidth + posX <= width) {

                            /*
                             * if a hyphenated word chain was being build,
                             * this is the <i>last</i> chunk of it
                             */
                            if (lastHyphenatedWord != null) {
                                HyphenatedTextRegion rt =
                                        new HyphenatedTextRegion(
                                        (short) 0, (short) 0,
                                        (short) wordPixelWidth,
                                        (short) fontHeight, parser.position,
                                        parser.length, style, color,
                                        lastHyphenatedWord);

                                /*
                                 * call RegionText.buildLinks() so that, the
                                 * chunks of text would be connected
                                 */
                                rt.buildLinks();
                                lastHyphenatedWord = null;

                                wordsOnThisLine.addElement(rt);
                            } else {

                                /*
                                 * Just add a whole word to the line
                                 */
                                wordsOnThisLine.addElement(
                                        new TextRegion((short) 0, (short) 0,
                                        (short) wordPixelWidth,
                                        (short) fontHeight, parser.position,
                                        parser.length, style, color));
                            }

                            posX += wordPixelWidth;
                            firstWord = false;
                        } else {

                            /*
                             * try to hyphenate word
                             */
                            dashWidth = font.dashWidth;

                            ZLTextHyphenationInfo info =
                                    hyphenator.getInfo(buffer,
                                    parser.position, parser.length);

                            /*
                             * try to hyphenate word, so that the largest
                             * possible chunk is on this line
                             */

                            /*
                             * wordInfo.length - 2: starts from one before
                             * the last
                             */
                            for (int i = parser.length - 2; i > 0; i--) {
                                if (info.isHyphenationPossible(i)) {
                                    wordPixelWidth = font.charsWidth(buffer,
                                            parser.position, i) + dashWidth;

                                    /*
                                     * This part of the word fits on the line
                                     */
                                    if (wordPixelWidth < width - posX) {

                                        /*
                                         * If the word chunk already ends with a
                                         * dash, include it.
                                         */
                                        if (buffer[parser.position + i]
                                                == '-') {
                                            i++;
                                        }

                                        HyphenatedTextRegion rt =
                                                new HyphenatedTextRegion(
                                                (short) 0, (short) 0,
                                                (short) wordPixelWidth,
                                                (short) fontHeight,
                                                parser.position, i, style,
                                                color, lastHyphenatedWord);

                                        wordsOnThisLine.addElement(rt);
                                        lastHyphenatedWord = rt;
                                        parser.position += i;
                                        parser.length = 0;
                                        posX += wordPixelWidth;
                                        firstWord = false;

                                        /* the word was hyphented */
                                        break line;
                                    }
                                }
                            }

                            /*
                             * The word could not be hyphenated. Could it fit
                             * into a single line at all?
                             */
                            if (font.charsWidth(buffer, parser.position,
                                    parser.length) > width) {

                                /* This word neither hyphenates, nor does it
                                 * fit at all on a single line, so one should
                                 * force hyphanation on it!
                                 */
                                for (int i = parser.length - 2; i > 0; i--) {
                                    wordPixelWidth = font.charsWidth(buffer,
                                            parser.position, i) + dashWidth;

                                    if (wordPixelWidth < width - posX) {
                                        /*
                                         * If the word chunk already ends with a
                                         * dash, include it.
                                         */
                                        if (buffer[parser.position + i]
                                                == '-') {
                                            i++;
                                        }

                                        HyphenatedTextRegion rt =
                                                new HyphenatedTextRegion(
                                                (short) 0, (short) 0,
                                                (short) wordPixelWidth,
                                                (short) fontHeight,
                                                parser.position, i, style,
                                                color, lastHyphenatedWord);

                                        wordsOnThisLine.addElement(rt);
                                        lastHyphenatedWord = rt;
                                        parser.position += i;
                                        parser.length = 0;
                                        posX += wordPixelWidth;
                                        firstWord = false;
                                        break line;
                                    }
                                }
                            }

                            /*
                             * The word could fit on a line, so will leave it
                             * for the next line, and won't add anything here.
                             */
                            parser.length = 0;
                            break;
                        }

                        /*
                         * All the text could fit on one line. This is usually
                         * the case for alt text for images.
                         */
                    }

                positionWordsOnLine(wordsOnThisLine, width, posY, spaceWidth,
                        fontIndent, lineBreak, startsNewParagraph, center);
                startsNewParagraph = false;

                if (lineBreak) {
                    startsNewParagraph = true;
                }

                lineBreak = false;

                if (lastLine) {
                    lastLine = false;
                    break;
                }
                posY += fontHeight;
                firstLine = false;
            }

        if (imageRegion == null) {
            /*
             * save the params for the next page
             */
            ip.position = this.end = parser.position;
            ip.length = parser.length;
            ip.style = style;
            ip.center = center;
            ip.lastHyphenatedWord = lastHyphenatedWord;
            ip.startsNewParagraph = startsNewParagraph;
        }
    }

    private void positionWordsOnLine(
            final Vector words,
                  int lineWidth,
            final int lineY,
            final int spaceWidth,
            final int fontIndent,
            final boolean endsParagraph,
            final boolean startsNewParagraph,
            final boolean center) {

        final int wordsSize = words.size();
        final int wordSpacing = spaceWidth;

        final byte align = (center ? CENTER : (endsParagraph) ? LEFT : JUSTIFY);

        if (wordsSize > 0) {
            int textWidth = 0;
            int x = 0;
            if (startsNewParagraph) {
                lineWidth = lineWidth - fontIndent;
                x = fontIndent;
            }

            for (int i = 0; i < wordsSize; i++) {
                TextRegion word = (TextRegion) words.elementAt(i);
                textWidth += word.width; //compute width without spaces
            }

            int spacing = 0;

            /* set spacing */
            if (align != JUSTIFY) {
                spacing = wordSpacing;
            } else {
                /* calculate spacing so words would be justified */
                if (words.size() > 1) {
                    spacing = (lineWidth - textWidth)/(wordsSize-1);
                }
            }
            
            /* calc X so that the block would be centered */
            if (align == CENTER) {
                x = (lineWidth - (textWidth + (spacing * (wordsSize-1))))/2;
            }

//            /* align right */
//            if (align == RIGHT) {
//                x = (lineWidth - (textWidth + (spacing * (wordsSize-1))));
//            }

            for (int i=0; i<wordsSize; i++) {
                TextRegion word = (TextRegion)words.elementAt(i);

                word.x = (short)x;
                word.y = (short)lineY;

                x += word.width + spacing;

                regions.addElement(word);
            }
        }
    }

    public final int getStart() {
        return start;
    }

    public final int getEnd() {
        return end;
    }

    public final boolean contains(final int position) {
        return start <= position && position < end;
    }

    public final Region getRegionAt(final int x, final int y) {
        Region current = null;
        int regionsSize = regions.size();
        for (int i = 0; i < regionsSize; i++) {
            current = (Region) regions.elementAt(i);
            if (current.containsPoint2D(x, y)) {
                return current;
            }
        }
        return null;
    }

    public final boolean isEmpty() {
        return regions.isEmpty();
    }

    public final void draw(
            final Graphics g,
            final ColorScheme cp,
            final AlbiteFont fontPlain,
            final AlbiteFont fontItalic,
            final char[] textBuffer) {

        final int regionsSize = regions.size();

        if (imageRegion != null) {
            /*
             * center vertically text & image
            */

            int textTopCorner = 0;
            int textHeight = 0;

            if (regionsSize > 0) {
                /*
                 * There is alt text
                 */
                textTopCorner = ((Region) regions.firstElement()).y;
                final Region r = (Region) regions.lastElement();
                textHeight = r.y + r.height - textTopCorner;
            }

            Image image = imageRegion.getImage(
                    booklet.width, booklet.height, textHeight);

            final int margin = ImageRegion.MARGIN;

            final int imageW = image.getWidth() + 4 * margin + 2;
            final int imageH = image.getHeight() + 4 * margin + 2;

            final int imageX = (booklet.width - imageW) / 2;
                  int imageY = (booklet.height - imageH) / 2;

            if (regionsSize > 0) {
                /*
                 * There is alt text
                 */
                final int h = imageH + textHeight;

                int offset = (booklet.height - h) / 2;

                imageY = offset;

                offset += imageH - textTopCorner;

                for (int i = 0; i < regionsSize; i++) {
                    ((Region) regions.elementAt(i)).y += offset;
                }
            }

            g.setColor(cp.colors[ColorScheme.COLOR_FRAME]);
            g.drawRect(
                    imageX + margin,
                    imageY + margin,
                    image.getWidth()  + 2 * margin + 1,
                    image.getHeight() + 2 * margin + 1);

            g.drawImage(image,
                    imageX + 2 * margin + 1,
                    imageY + 2 * margin + 1,
                    Graphics.TOP | Graphics.LEFT);
        }

        /*
         * drawing regions in a normal page
         */
        for (int i = 0; i < regionsSize; i++) {
            Region region = (Region) regions.elementAt(i);
            region.draw(g, cp, fontPlain, fontItalic, textBuffer);
        }
    }

    public final String getFirstWord(final char[] chapterBuffer) {

        final int size = regions.size();

        for (int i = 0; i < size; i++) {
            Region r = (Region) regions.elementAt(i);
            if (r instanceof TextRegion) {
                return ((TextRegion) r).getText(chapterBuffer);
            }
        }

        return "";
    }

    public static AlbiteFont chooseFont(
            final AlbiteFont fontPlain,
            final AlbiteFont fontItalic,
            final byte style) {

        AlbiteFont font = fontPlain;
        if ((style & ITALIC) == ITALIC) {
            font = fontItalic;
        }

        return font;
    }

    public static byte chooseTextColor(final byte style) {
        byte color = ColorScheme.COLOR_TEXT;

        if ((style & ITALIC) == ITALIC) {
            color = ColorScheme.COLOR_TEXT_ITALIC;
        }

        if ((style & BOLD) == BOLD) {
            color = ColorScheme.COLOR_TEXT_BOLD;
        }

        if ((style & HEADING) == HEADING) {
            color = ColorScheme.COLOR_TEXT_HEADING;
        }

        return color;
    }
}