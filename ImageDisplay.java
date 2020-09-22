
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.LinkedList;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage imgOne;
	BufferedImage imgTwo;
	int width = 512;
	int height = 512;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	class RGB {
		int r, g, b;
		public RGB(int r, int g, int b){
			this.r = r;
			this.g= g;
			this.b = b;
		}
	}

	RGB[][] originalRGB = new RGB[height][width];
	//RGB[][] quantized = new RGB[height][width];
	RGB[][] filtered = new RGB[height][width];
	RGB[][] scaled = null;
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];


			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2];
					int R = bytes[ind];
					int G = bytes[ind+height*width];
					int B = bytes[ind+height*width*2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);

					//convert to unsigned int
					R = R & 0xFF;
					G = G & 0xFF;
					B = B & 0xFF;

					//Store original RGB values to use later
					RGB rgb = new RGB(R, G, B);
					originalRGB[x][y] = rgb;
					ind++;
				}
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){

		// Read a parameter from command line
		String param1 = args[1];
		System.out.println("Scaling factor " + param1);
		System.out.println("No of units " + args[2]);
		System.out.println("Quantization Mode " + args[3]);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Filtered Image
		filter();

		// Scaled Image
		double scaleFactor = Double.parseDouble(args[1]);
		int newHeight = (int) (height * scaleFactor);
		int newWidth = (int) (width * scaleFactor);
		scaled = new RGB[newHeight][newWidth];
		imgTwo = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		scale(scaleFactor,newHeight,newWidth);

		// Uniform Quantization
		if(args[3].equalsIgnoreCase("-1")) {
			boolean doQuantization = true;
			Integer[] buckets = null;
			double new_bits = Double.parseDouble(args[2]);
			double slotSize = 256/Math.pow(2,new_bits);
			buckets = getBucketsForQuantization(slotSize);
			System.out.println("Reached here : Uniform Quantization");
			for(int y = 0; y < newHeight; y++) {
				for (int x = 0; x < newWidth; x++) {
					RGB rgb = scaled[y][x];
					int RED = rgb.r;
					int GREEN = rgb.g;
					int BLUE = rgb.b;
					if(doQuantization) {

						int[] quantizedRGB = quantize(RED, GREEN, BLUE, buckets);
						// Storing the quantized values in the original matrix.
						int R = quantizedRGB[0];
						int G = quantizedRGB[1];
						int B = quantizedRGB[2];
						int pix = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);
						imgTwo.setRGB(y, x, pix);

					}
				}
			}
		}
		else if(Integer.parseInt(args[3]) >= 0) {
			System.out.println("Reached here : Log Quantization");
			int offSet = Integer.parseInt(args[3]);
			int bits = Integer.parseInt(args[2]);
			double slotSize = 256/Math.pow(2,bits);
			Integer[] buckets = null;
			boolean doQuantization = true;
			buckets = getBucketsForQuantization(slotSize);
			Integer[] new_buckets = null;
			new_buckets = getBucketsForLogQuantization(buckets,offSet);
			for(int y = 0; y < newHeight; y++) {
				for (int x = 0; x < newWidth; x++) {
					RGB rgb = scaled[y][x];
					int RED = rgb.r;
					int GREEN = rgb.g;
					int BLUE = rgb.b;
					if(doQuantization) {
						int[] quantizedRGB = quantize(RED, GREEN, BLUE, new_buckets);
						// Storing the quantized values in the original matrix.
						int R = quantizedRGB[0];
						int G = quantizedRGB[1];
						int B = quantizedRGB[2];
						int pix = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);
						imgTwo.setRGB(y, x, pix);
					}
				}
			}
		}

		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(imgOne));
		lbIm2 = new JLabel(new ImageIcon(imgTwo));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

	}

	private Integer[] getBucketsForLogQuantization(Integer[] buckets, int mode) {
		LinkedList<Integer> list = new LinkedList<Integer>();
		int flag = 0;

		// Finding the mirror image if greater than 128.
		if(mode > 128) {
			flag = 1;
			mode = 256 - mode;
		}
		for ( int i = 0; i < buckets.length; i++) {
			double val = buckets[i];
			if(flag == 1) {
				val = 256 - val;
			}
			double x = (val-mode) / (256-mode);
			double new_val;
			double upper = Math.log(1 + 255 * (1-Math.abs(x)));
			double lower = Math.log(256);
			new_val = 1 - (upper/lower);
			if(x < 0) {
				new_val = new_val * -1;
			}
			double result = (new_val * (256-mode)) + mode;
			if(flag == 1) {
				result = 256 - result;
			}
			list.add((int) result);
		}
		Integer[] new_buckets = new Integer[list.size()];
		new_buckets = list.toArray(new_buckets);
		return new_buckets;
	}

	private void scale(double scaleFactor, int newHeight, int newWidth) {
		for(int y = 0; y < newHeight; y++) {
			for (int x = 0; x < newWidth; x++) {
				int x1 = (int) (x / scaleFactor);
				int y1 = (int) (y / scaleFactor);
				if(x1 >= 0 && y1 >= 0 && x1 < width && y1 < height) {
					RGB rgb = filtered[y1][x1];
					int R = rgb.r;
					int G = rgb.g;
					int B = rgb.b;
					RGB new_rgb = new RGB(R,G,B);
					scaled[x][y] = new_rgb;
				}
			}
		}
	}

	/**
	 * Calculate the quantization buckets as per step function
	 * @param step
	 * @return
	 */
	private Integer[] getBucketsForQuantization(double step) {
		LinkedList<Integer> list = new LinkedList<Integer>();
		double trueValue = 0;
		int value = 0;

		list.add(value);
		while(true){
			trueValue = trueValue + step;
			value = (int) Math.round(trueValue);

			if(value > 256){
				break;
			}
			list.add(value);
		}

		Integer[] buckets = new Integer[list.size()];
		buckets = list.toArray(buckets);
		return buckets;
	}

	/**
	 * Quantization Function to quantize the R,G,B values
	 * @param R
	 * @param G
	 * @param B
	 * @param buckets
	 * @return
	 */
	private int[] quantize(int R, int G, int B, Integer[] buckets) {
		for(int i=0; i < buckets.length-1; i++) {
			if(R >= buckets[i] && R <= buckets[i+1]){
				int mean = (int) Math.round((buckets[i] + buckets[i+1])/(double)2);
				R = mean;
				break;
			}
		}
		if(R > 255){
			R = 255;
		}else if(R < 0){
			R = 0;
		}

		for(int i=0; i < buckets.length-1; i++){
			if(G >= buckets[i] && G <= buckets[i+1]){
				int mean = (int) Math.round((buckets[i] + buckets[i+1])/(double)2);
				G = mean;
				break;
			}
		}
		if(G > 255){
			G = 255;
		}else if(G < 0){
			G = 0;
		}

		for(int i=0; i < buckets.length-1; i++){
			if(B >= buckets[i] && B <= buckets[i+1]){
				int mean = (int) Math.round((buckets[i] + buckets[i+1])/(double)2);
				B = mean;
				break;
			}
		}
		if(B > 255){
			B = 255;
		}else if(B < 0){
			B = 0;
		}
		return new int[]{R, G, B};
	}

	private void filter() {
		for(int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int[] filterPixel = kernelFilter(x , y ,height, width);
				RGB filteredRGB = new RGB(filterPixel[0],filterPixel[1],filterPixel[2]);
				filtered[x][y] = filteredRGB;
			}
		}
	}

	private int[] kernelFilter(int x, int y, int height, int width) {
		int[][] dirs = {{-1,-1},{0,-1},{1,-1},{-1,0},{0,0},{1,0},{-1,1},{0,1},{1,1}};

		double sumR = 0;
		double sumG = 0;
		double sumB = 0;

		for(int[] dir : dirs) {
			int x1 = x + dir[0];
			int y1 = y + dir[1];
			if(x1 >= 0 && y1 >= 0 && x1 < width && y1 < height) {
				RGB rgb = originalRGB[y1][x1];
				int oldR = rgb.r;
				int oldG = rgb.g;
				int oldB = rgb.b;
				double newR = oldR/9;
				double newG = oldG/9;
				double newB = oldB/9;
				sumR += newR;
				sumG += newG;
				sumB += newB;
			}
		}

		int red = (int) sumR;
		int green = (int) sumG;
		int blue = (int) sumB;
		return new int[] {red,green,blue};
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}

