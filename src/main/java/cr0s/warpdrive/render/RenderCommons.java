package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.EnumDisplayAlignment;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.TextComponentTranslation;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCommons {
	
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	protected static int colorGradient(final float gradient, final int start, final int end) {
		return Math.max(0, Math.min(255, start + Math.round(gradient * (end - start))));
	}
	
	// from net.minecraft.client.gui.Gui
	private static final float scaleUV = 0.00390625F;  // 1/256
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v, final int sizeX, final int sizeY, final int zLevel) {
		final Tessellator tessellator = Tessellator.getInstance();
		final VertexBuffer vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		vertexBuffer.pos( x         , (y + sizeY), zLevel).tex(scaleUV * u          , scaleUV * (v + sizeY)).endVertex();
		vertexBuffer.pos((x + sizeX), (y + sizeY), zLevel).tex(scaleUV * (u + sizeX), scaleUV * (v + sizeY)).endVertex();
		vertexBuffer.pos((x + sizeX),  y         , zLevel).tex(scaleUV * (u + sizeX), scaleUV *  v         ).endVertex();
		vertexBuffer.pos( x         ,  y         , zLevel).tex(scaleUV * u          , scaleUV *  v         ).endVertex();
		tessellator.draw();
	}
	
	public static int drawSplashAlarm(final int scaledWidth, final int scaledHeight, final String title, final String message) {
		// compute animation clock
		final double cycle = ((System.nanoTime() / 1000) % 0x200000) / (double) 0x200000;
		
		// start rendering
		GL11.glPushMatrix();
		GL11.glScalef(2.0F, 2.0F, 0.0F);
		
		int y = scaledHeight / 10;
		
		// bold title, single line, centered, with shadows
		final String textTitle = Commons.updateEscapeCodes("§l" + new TextComponentTranslation(title).getFormattedText());
		minecraft.fontRendererObj.drawString(textTitle,
		                                  scaledWidth / 4 - minecraft.fontRendererObj.getStringWidth(textTitle) / 2,
		                                  y - minecraft.fontRendererObj.FONT_HEIGHT,
		                                     Commons.colorARGBtoInt(230, 255, 32, 24),
		                                  true);
		
		// normal message, multi-lines, centered, without shadows
		final String textMessage = Commons.updateEscapeCodes(new TextComponentTranslation(message).getFormattedText());
		final int alpha = 160 + (int) (85.0D * Math.sin(cycle * 2 * Math.PI));
		
		@SuppressWarnings("unchecked")
		final List<String> listMessages = minecraft.fontRendererObj.listFormattedStringToWidth(textMessage, scaledWidth / 2);
		for (final String textLine : listMessages) {
			minecraft.fontRendererObj.drawString(textLine,
			                                  scaledWidth / 4 - minecraft.fontRendererObj.getStringWidth(textLine) / 2,
			                                  y,
			                                  Commons.colorARGBtoInt(alpha, 192, 64, 48),
			                                  false);
			y += minecraft.fontRendererObj.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
		return alpha;
	}
	
	private static final int TEXT_BORDER = 2;
	public static void drawText(final int screen_width, final int screen_height, final String text,
	                           final float scale, final String formatPrefix, final int colorBackground, final int colorText, final boolean hasShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String text_formatted = Commons.updateEscapeCodes(formatPrefix + new TextComponentTranslation(text).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		@SuppressWarnings("unchecked")
		final List<String> listLines = minecraft.fontRendererObj.listFormattedStringToWidth(text_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final int scaled_box_height = listLines.size() * minecraft.fontRendererObj.FONT_HEIGHT + 2 * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		final byte red   = (byte) (colorBackground >> 16 & 255);
		final byte blue  = (byte) (colorBackground >> 8 & 255);
		final byte green = (byte) (colorBackground & 255);
		final byte alpha = (byte) (colorBackground >> 24 & 255);
		GL11.glColor4b(red, blue, green, alpha);
		
		final Tessellator tessellator = Tessellator.getInstance();
		final VertexBuffer vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).endVertex();
		tessellator.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		
		// draw text
		for (final String textLine : listLines) {
			minecraft.fontRendererObj.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasShadow);
			scaled_text_y += minecraft.fontRendererObj.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String textHeader, final String textContent,
	                           final float scale, final String formatHeader, final int colorBackground, final int colorText, final boolean hasHeaderShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String header_formatted  = Commons.updateEscapeCodes(String.format(new TextComponentTranslation(textHeader, formatHeader).getFormattedText()));
		final String content_formatted = Commons.updateEscapeCodes(new TextComponentTranslation(textContent).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		@SuppressWarnings("unchecked")
		final List<String> listHeaderLines = 
			header_formatted.isEmpty() ? new ArrayList<>(0)
			                           : minecraft.fontRendererObj.listFormattedStringToWidth(header_formatted, scaled_box_width - 2 * TEXT_BORDER);
		@SuppressWarnings("unchecked")
		final List<String> listContentLines =
			content_formatted.isEmpty() ? new ArrayList<>(0)
		                                : minecraft.fontRendererObj.listFormattedStringToWidth(content_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final boolean hasTileAndContent = listHeaderLines.size() > 0 && listContentLines.size() > 0;
		final int scaled_box_height = (listHeaderLines.size() + listContentLines.size()) * minecraft.fontRendererObj.FONT_HEIGHT
		                            + (hasTileAndContent ? 3 : 1) * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		final byte red   = (byte) (colorBackground >> 16 & 255);
		final byte blue  = (byte) (colorBackground >> 8 & 255);
		final byte green = (byte) (colorBackground & 255);
		final byte alpha = (byte) (colorBackground >> 24 & 255);
		GL11.glColor4b(red, blue, green, alpha);
		
		final Tessellator tessellator = Tessellator.getInstance();
		final VertexBuffer vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).endVertex();
		tessellator.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		
		// draw text
		for (final String textLine : listHeaderLines) {
			minecraft.fontRendererObj.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasHeaderShadow);
			scaled_text_y += minecraft.fontRendererObj.FONT_HEIGHT;
		}
		if (hasTileAndContent) {
			scaled_text_y += TEXT_BORDER;
		}
		for (final String textLine : listContentLines) {
			minecraft.fontRendererObj.drawString(textLine, scaled_text_x, scaled_text_y, colorText, false);
			scaled_text_y += minecraft.fontRendererObj.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
	}
}